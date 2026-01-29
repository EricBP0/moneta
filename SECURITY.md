# Security Considerations

## Authentication Token Storage

### Current Implementation
Authentication tokens (accessToken and refreshToken) are currently stored in `localStorage`. This approach has the following characteristics:

**Pros:**
- Simple to implement
- Works well for client-side only applications
- Easy to access from JavaScript

**Security Considerations:**
- Vulnerable to XSS (Cross-Site Scripting) attacks
- If an attacker can inject JavaScript into the application, they can steal these tokens
- Tokens persist across browser sessions

### Recommended Mitigations

To minimize security risks with the current approach:

1. **XSS Prevention Measures:**
   - Never render unsanitized user input directly in the DOM
   - Use React's built-in XSS protections (JSX escaping)
   - Avoid using `dangerouslySetInnerHTML` unless absolutely necessary
   - Implement Content Security Policy (CSP) headers
   - Sanitize all user inputs on both client and server side

2. **Token Lifecycle Management:**
   - Implement short-lived access tokens (current implementation uses refresh tokens)
   - Clear tokens immediately on logout
   - Implement automatic session expiration
   - Consider implementing token rotation

3. **Alternative Storage Options (Future Consideration):**
   - **HttpOnly Cookies:** Most secure option, immune to XSS but requires server-side session management
   - **SessionStorage:** More secure than localStorage as it clears on tab close
   - **Memory-only storage:** Most secure but lost on page refresh

### Current Security Features

The application currently implements:

- ✅ SSR-safe localStorage access with `typeof window !== 'undefined'` checks
- ✅ Automatic token refresh on 401 responses
- ✅ Prevention of multiple simultaneous refresh attempts
- ✅ Automatic redirect to login on session expiration
- ✅ Token cleanup on logout

### Future Improvements

Consider implementing:
- [ ] HttpOnly cookie-based authentication
- [ ] Content Security Policy headers
- [ ] Token rotation on refresh
- [ ] Rate limiting for authentication endpoints
- [ ] Audit logging for authentication events

## Race Condition Handling

### Token Refresh Race Condition

The `apiClient` implements a singleton pattern to prevent race conditions during token refresh:

```typescript
let refreshPromise: Promise<AuthResponse> | null = null

// In the request function:
if (!refreshPromise) {
  refreshPromise = refreshSession().finally(() => {
    refreshPromise = null
  })
}
await refreshPromise
```

**How it works:**
1. When a 401 response is received, the code checks if a refresh is already in progress
2. If yes, it waits for the existing refresh promise to complete
3. If no, it creates a new refresh promise
4. The promise is cleared after completion (success or failure)
5. All waiting requests then retry with the new token

**Edge Cases Handled:**
- Multiple concurrent requests failing with 401
- Requests arriving during token refresh
- Refresh failure (all requests are redirected to login)

**Known Limitations:**
- If a refresh succeeds and immediately another 401 occurs before the promise is cleared, there's a small window where another refresh might be triggered
- This is acceptable because the refresh endpoint should be idempotent

**Testing Recommendations:**
- Test concurrent API calls with expired tokens
- Test behavior when refresh endpoint fails
- Test behavior when refresh endpoint is slow
