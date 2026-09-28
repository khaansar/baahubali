# Refresh Token Flow — client + IAM

## 1. Access Token

client sends an access token with every API request:

```text
client → API Gateway → Service
          Bearer A1
```

The access token is **short-lived** (for example, 10–15 minutes).

Why? If it is stolen, it should only be useful for a limited time.

---

## 2. Access Token Expires

After A1 expires:

```text
API Request
    ↓
401 Unauthorized
```

We don't want the user to log in again.

So we use a **refresh token**.

---

## 3. Refresh Token

At login:

```text
Access Token  = A1   (short-lived)
Refresh Token = R1   (long-lived)
```

The refresh token is stored in an **HttpOnly + Secure cookie**.

When A1 expires:

```text
client
   ↓
POST /auth/refresh
   ↓
IAM
   ↓
New Access Token A2
```

---

## 4. Refresh Token Rotation

We should not keep using R1 forever.

Instead:

```text
R1 → R2 → R3 → R4
```

Every successful refresh:

```text
Old refresh token → revoked
New refresh token → generated
New access token → generated
```

This lets IAM detect if an old refresh token is reused.

---

## 5. The Race Condition

Now consider a page loading several APIs:

```text
Page Load
   ├── /me          → 401
   ├── /tests       → 401
   ├── /categories  → 401
   └── /questions   → 401
```

If every request calls refresh:

```text
401 → refresh R1
401 → refresh R1
401 → refresh R1
401 → refresh R1
```

This is a problem because R1 is being rotated.

---

## 6. Solution: Single-Flight Refresh

The frontend keeps one shared refresh operation.

```text
/me          → 401 ──┐
/tests       → 401 ──┤
/categories  → 401 ──┤
/questions   → 401 ──┘
                     ↓
               ONE refresh
                     ↓
                  A2 + R2
                     ↓
          ┌──────────┼──────────┐
          ↓          ↓          ↓
        retry      retry      retry
```

The first request starts the refresh.

The other requests **wait for the same refresh promise** instead of starting another refresh.

This is often called **single-flight** or a **refresh queue**.

---

## 7. Final Design

```text
                 client
                   |
             API request
                   |
              Access A1
                   |
              401 expired
                   |
          Refresh Coordinator
                   |
            ONE refresh only
                   |
                   ↓
            POST /auth/refresh
                   |
                   ↓
                  IAM
                   |
             R1 → R2
             A1 → A2
                   |
                   ↓
             Retry requests
```

### Responsibilities

**client / Next.js**

- Detect `401`
- Ensure only one refresh runs at a time
- Make other requests wait
- Retry them with the new access token
- Send the user to login if refresh fails

**IAM**

- Validate refresh token
- Rotate refresh token
- Store token hashes
- Detect refresh-token reuse
- Revoke sessions/token families when necessary

## In short

> **Access token expires → client receives 401 → one refresh request is made → IAM rotates R1 to R2 and creates A2 → waiting requests retry with A2.**
