/**
 * 统一响应包与分页结构（契约 4.4）
 * Result 标注 @JsonInclude(NON_NULL)：data/requestId 为 null 时字段整体缺失（契约 3.4），一律可选
 */
export interface Result<T = unknown> {
  code: number
  message: string
  data?: T
  /** 后端由 RequestIdFilter 回填；缺省时不参与业务判断（契约 4.3） */
  requestId?: string
  /** yyyy-MM-dd HH:mm:ss（Jackson date-format + Asia/Shanghai），原样直显（契约 3.3） */
  timestamp?: string
}

/** 分页响应（契约 4.4 / D4：只适配 PageResult 一种结构，camelCase） */
export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  list: T[]
}

export interface PageQuery {
  page?: number
  size?: number
}

export interface DictOption<T extends string | number = string> {
  label: string
  value: T
  disabled?: boolean
}
