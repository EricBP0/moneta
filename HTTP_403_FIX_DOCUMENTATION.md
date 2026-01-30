# HTTP 403 Error Fix - Security Configuration

## Root Cause Analysis

The application was experiencing HTTP 403 (Forbidden) errors on endpoints that should have been accessible. The investigation revealed several configuration issues:

### Issues Found:

1. **Missing Exception Handlers**: Spring Security was not configured with custom `AuthenticationEntryPoint` and `AccessDeniedHandler`, leading to incorrect HTTP status codes:
   - Requests without authentication were sometimes returning 403 instead of 401
   - No structured JSON error responses for authentication failures

2. **JWT Filter Processing Public Routes**: The JWT authentication filter was processing all requests, including public routes like `/api/auth/**` and `/actuator/health`, causing unnecessary overhead and potential issues.

3. **Lack of Debug Logging**: No logging configuration for Spring Security made troubleshooting difficult.

## Solutions Implemented

### 1. Custom Exception Handlers

**File: `CustomAuthenticationEntryPoint.java`**
- Returns proper 401 Unauthorized status with JSON error response
- Triggered when unauthenticated users try to access protected resources
- Provides clear error message: "Authentication required to access this resource"

**File: `CustomAccessDeniedHandler.java`**
- Returns proper 403 Forbidden status with JSON error response
- Triggered when authenticated users lack required permissions
- Provides clear error message: "You don't have permission to access this resource"

### 2. Updated Security Configuration

**File: `SecurityConfig.java`**

Added exception handling configuration:
```java
.exceptionHandling(exceptions -> exceptions
  .authenticationEntryPoint(authenticationEntryPoint)  // 401 for unauthenticated
  .accessDeniedHandler(accessDeniedHandler)           // 403 for unauthorized
)
```

This ensures:
- **401 Unauthorized**: No token, invalid token, or expired token
- **403 Forbidden**: Authenticated but lacking required permissions (future RBAC feature)

### 3. Optimized JWT Filter

**File: `JwtAuthenticationFilter.java`**

Added `shouldNotFilter()` method to skip processing for:
- Public paths: `/api/auth/**`, `/actuator/health`, `/actuator/info`
- OPTIONS requests (CORS preflight)

Benefits:
- Reduced processing overhead for public endpoints
- Cleaner separation between public and protected routes
- No JWT validation attempted on routes that don't require authentication

### 4. Security Debug Logging

**File: `application.yml`**

Added configurable logging levels:
```yaml
logging:
  level:
    org.springframework.security: ${SECURITY_LOG_LEVEL:INFO}
    org.springframework.web.cors: ${CORS_LOG_LEVEL:INFO}
    com.moneta.config: ${APP_CONFIG_LOG_LEVEL:INFO}
```

Can be enabled for debugging:
```bash
SECURITY_LOG_LEVEL=DEBUG CORS_LOG_LEVEL=DEBUG
```

## Security Configuration Summary

### Public Endpoints (No Authentication Required)
- `OPTIONS /**` - CORS preflight requests
- `/api/auth/**` - Authentication endpoints (login, register, refresh)
- `/actuator/health` - Health check endpoint
- `/actuator/info` - Application info endpoint

### Protected Endpoints (Authentication Required)
- `/api/dashboard/**` - Dashboard data
- `/api/budgets/**` - Budget management
- `/api/transactions/**` - Transaction management
- `/api/categories/**` - Category management
- `/api/goals/**` - Goal management
- `/api/rules/**` - Rule management
- `/api/accounts/**` - Account management
- `/api/institutions/**` - Institution management
- `/api/alerts/**` - Alert management
- All other `/api/**` endpoints

### CORS Configuration
The application already had proper CORS configuration in `CorsConfig.java`:
- Allowed origins: Vercel (`*.vercel.app`), production domains, localhost
- Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Allowed headers: Content-Type, Authorization, Accept
- Credentials: false (JWT in body, not cookies)

## Testing

Created comprehensive test suite: `SecurityStatusCodeIntegrationTest.java`

Tests verify:
1. ✅ Protected endpoints return 401 without token
2. ✅ Protected endpoints return 401 with invalid token
3. ✅ Public endpoints are accessible without token
4. ✅ Health endpoint is accessible without token
5. ✅ Protected endpoints work with valid token

All existing tests continue to pass (65/65).

## Endpoints Status Code Behavior

| Scenario | Status Code | Response |
|----------|-------------|----------|
| Access protected endpoint without token | 401 | `{"status": 401, "error": "Unauthorized", "message": "Authentication required..."}` |
| Access protected endpoint with invalid/expired token | 401 | Same as above |
| Access protected endpoint with valid token | 200/201/204 | Success response |
| Access public endpoint without token | 200/201 | Success response |
| CORS preflight (OPTIONS) | 200/204 | CORS headers included |
| Authenticated but lacking permission (future) | 403 | `{"status": 403, "error": "Forbidden", "message": "You don't have permission..."}` |

## Migration Notes

### For Frontend Developers
No breaking changes. The API now returns more consistent status codes:
- Always check for 401 → redirect to login
- 403 will be used in the future for permission issues (currently not implemented)

### For DevOps
Environment variables for debugging:
```bash
SECURITY_LOG_LEVEL=DEBUG  # Enable Spring Security debug logging
CORS_LOG_LEVEL=DEBUG      # Enable CORS debug logging
APP_CONFIG_LOG_LEVEL=DEBUG  # Enable application config debug logging
```

## Files Changed

1. **New Files:**
   - `backend/src/main/java/com/moneta/config/CustomAuthenticationEntryPoint.java`
   - `backend/src/main/java/com/moneta/config/CustomAccessDeniedHandler.java`
   - `backend/src/test/java/com/moneta/integration/SecurityStatusCodeIntegrationTest.java`

2. **Modified Files:**
   - `backend/src/main/java/com/moneta/config/SecurityConfig.java`
   - `backend/src/main/java/com/moneta/config/JwtAuthenticationFilter.java`
   - `backend/src/main/resources/application.yml`

## Verification Steps

1. **Build:** `mvn clean compile` ✅
2. **All Tests:** `mvn test` - 65/65 passed ✅
3. **CORS Tests:** `mvn test -Dtest=CorsIntegrationTest` - 6/6 passed ✅
4. **Auth Tests:** `mvn test -Dtest=AuthFlowIntegrationTest` - 1/1 passed ✅
5. **Security Tests:** `mvn test -Dtest=SecurityStatusCodeIntegrationTest` - 5/5 passed ✅

## Future Enhancements

While the current implementation addresses the HTTP 403 issues, future enhancements could include:

1. **Role-Based Access Control (RBAC):**
   - Add roles to User entity (e.g., USER, ADMIN)
   - Implement role-based endpoint protection
   - Use 403 for role-based access denials

2. **Enhanced Logging:**
   - Add custom filters to log all authentication attempts
   - Track failed authentication patterns
   - Monitor CORS violations

3. **Rate Limiting:**
   - Implement rate limiting for authentication endpoints
   - Prevent brute force attacks
   - Return 429 Too Many Requests when exceeded

## Conclusion

The HTTP 403 errors were caused by missing exception handlers in Spring Security configuration. The fixes ensure:
- ✅ Correct HTTP status codes (401 for authentication, 403 for authorization)
- ✅ Proper JSON error responses
- ✅ Optimized JWT filter processing
- ✅ Public endpoints remain accessible
- ✅ Protected endpoints properly secured
- ✅ CORS continues to work correctly
- ✅ All tests pass without regressions
