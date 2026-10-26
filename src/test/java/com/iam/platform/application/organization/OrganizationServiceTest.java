package com.iam.platform.application.organization;

import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamOrg;
import com.iam.platform.infrastructure.persistence.repository.IamOrgRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 组织树：1 总公司(0) → 2 分公司(0,1) → 3 部门(0,1,2)；12 另一分公司(0,1)，用于验证前缀不误匹配。
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private IamOrgRepository orgRepository;
    @InjectMocks
    private OrganizationService service;

    private static IamOrg org(Long id, Long parentId, String ancestors, String type, int level) {
        IamOrg o = new IamOrg();
        o.setId(id);
        o.setParentId(parentId);
        o.setAncestors(ancestors);
        o.setOrgCode("ORG" + id);
        o.setOrgType(type);
        o.setOrgLevel(level);
        o.setSortNo(0);
        o.setIsDeleted(0);
        return o;
    }

    private static int code(Throwable e) {
        return ((BusinessException) e).getCode();
    }

    @Test
    void createRootAndChildComputeAncestors() {
        IamOrg hq = org(1L, null, "0", "HQ", 1);
        when(orgRepository.findByOrgCode(any())).thenReturn(Optional.empty());
        when(orgRepository.findById(1L)).thenReturn(Optional.of(hq));
        when(orgRepository.save(any(IamOrg.class))).thenAnswer(inv -> inv.getArgument(0));

        IamOrg root = service.createOrg("HQ1", "总公司", "HQ", 1, null, null);
        IamOrg branch = service.createOrg("BR1", "桥梁分公司", "BRANCH", 2, 1L, 3);

        assertThat(root.getAncestors()).isEqualTo("0");
        assertThat(root.getStatus()).isEqualTo("ACTIVE");
        assertThat(branch.getAncestors()).isEqualTo("0,1");
        assertThat(branch.getSortNo()).isEqualTo(3);
    }

    @Test
    void createRejectsLevelMismatchDuplicateAndLowerParent() {
        IamOrg dept = org(3L, 2L, "0,1,2", "DEPT", 3);
        when(orgRepository.findByOrgCode("DUP")).thenReturn(Optional.of(new IamOrg()));
        when(orgRepository.findByOrgCode("NEW")).thenReturn(Optional.empty());
        when(orgRepository.findById(3L)).thenReturn(Optional.of(dept));

        assertThatThrownBy(() -> service.createOrg("X", "x", "BRANCH", 3, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.PARAM_INVALID.getCode()));
        assertThatThrownBy(() -> service.createOrg("DUP", "x", "HQ", 1, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_DUPLICATE.getCode()));
        assertThatThrownBy(() -> service.createOrg("NEW", "x", "BRANCH", 2, 3L, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.PARAM_INVALID.getCode()));
        assertThatThrownBy(() -> service.createOrg("NEW", "x", "UNKNOWN", 1, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orgRepository, never()).save(any());
    }

    @Test
    void treeWithoutRootReturnsTopLevelAndWithRootReturnsSubtree() {
        IamOrg hq = org(1L, null, "0", "HQ", 1);
        IamOrg branch = org(2L, 1L, "0,1", "BRANCH", 2);
        IamOrg dept = org(3L, 2L, "0,1,2", "DEPT", 3);
        IamOrg other = org(12L, 1L, "0,1", "BRANCH", 2);
        IamOrg deleted = org(9L, null, "0", "HQ", 1);
        deleted.setIsDeleted(1);
        when(orgRepository.findByParentIdIsNull()).thenReturn(List.of(hq, deleted));
        when(orgRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(orgRepository.findByAncestorsStartingWith("0,1,2")).thenReturn(List.of(dept));

        assertThat(service.getOrgTree(null)).containsExactly(hq);
        assertThat(service.getOrgTree(2L)).containsExactly(branch, dept).doesNotContain(other);
    }

    @Test
    void childrenRequireExistingParent() {
        IamOrg hq = org(1L, null, "0", "HQ", 1);
        IamOrg branch = org(2L, 1L, "0,1", "BRANCH", 2);
        when(orgRepository.findById(1L)).thenReturn(Optional.of(hq));
        when(orgRepository.findById(99L)).thenReturn(Optional.empty());
        when(orgRepository.findByParentId(1L)).thenReturn(List.of(branch));

        assertThat(service.getChildren(1L)).containsExactly(branch);
        assertThatThrownBy(() -> service.getChildren(99L))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void moveUpdatesNodeAndDescendantAncestors() {
        IamOrg dept = org(3L, 2L, "0,1,2", "DEPT", 3);
        IamOrg project = org(4L, 3L, "0,1,2,3", "PROJECT", 4);
        IamOrg prefixLookalike = org(30L, 3L, "0,1,2,30", "PROJECT", 4); // 路径以 0,1,2,3 开头但不是下级
        IamOrg newBranch = org(12L, 1L, "0,1", "BRANCH", 2);
        when(orgRepository.findById(3L)).thenReturn(Optional.of(dept));
        when(orgRepository.findById(12L)).thenReturn(Optional.of(newBranch));
        when(orgRepository.findByAncestorsStartingWith("0,1,2,3")).thenReturn(List.of(project, prefixLookalike));
        when(orgRepository.save(dept)).thenReturn(dept);

        service.moveOrg(3L, 12L);

        assertThat(dept.getParentId()).isEqualTo(12L);
        assertThat(dept.getAncestors()).isEqualTo("0,1,12");
        assertThat(project.getAncestors()).isEqualTo("0,1,12,3");
        assertThat(prefixLookalike.getAncestors()).isEqualTo("0,1,2,30");
    }

    @Test
    void moveRejectsCycleSelfAndCrossLevel() {
        IamOrg branch = org(2L, 1L, "0,1", "BRANCH", 2);
        IamOrg dept = org(3L, 2L, "0,1,2", "DEPT", 3);
        IamOrg otherDept = org(5L, 12L, "0,1,12", "DEPT", 3);
        when(orgRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(orgRepository.findById(3L)).thenReturn(Optional.of(dept));
        when(orgRepository.findById(5L)).thenReturn(Optional.of(otherDept));

        assertThatThrownBy(() -> service.moveOrg(2L, 3L))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.ORG_MOVE_INVALID.getCode()));
        assertThatThrownBy(() -> service.moveOrg(2L, 2L))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.ORG_MOVE_INVALID.getCode()));
        assertThatThrownBy(() -> service.moveOrg(3L, 5L))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.ORG_MOVE_INVALID.getCode()));
        verify(orgRepository, never()).save(any());
    }
}
