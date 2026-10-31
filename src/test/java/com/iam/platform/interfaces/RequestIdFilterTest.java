package com.iam.platform.interfaces;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void propagatesRequestIdToHeaderAttributeAndMdcThenClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/roles");
        request.addHeader(RequestIdFilter.HEADER, "req-test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observedInChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> observedInChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertEquals("req-test", response.getHeader(RequestIdFilter.HEADER));
        assertEquals("req-test", request.getAttribute(RequestIdFilter.ATTR_REQUEST_ID));
        assertEquals("req-test", observedInChain.get());
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void unsafeIncomingRequestIdIsReplaced() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/roles");
        request.addHeader(RequestIdFilter.HEADER, "bad id with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        String generated = response.getHeader(RequestIdFilter.HEADER);
        assertNotNull(generated);
        assertTrue(generated.matches("[A-Za-z0-9._:-]+"));
    }
}
