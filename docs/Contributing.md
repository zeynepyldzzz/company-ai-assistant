# Contribution Guidelines

This document covers everything you need to know to contribute to the **AI-Powered Internal Assistant** platform (officeChatbot) — from setting up your environment to getting a PR merged.

---

## Getting Started

Before writing any code, make sure your local environment is running correctly.

> See **[`tech_stack.md`](../tech_stack.md)** for the required tech stack (Java 21 + Spring Boot 3 backend, React + TypeScript + Vite web/admin, PostgreSQL + Flyway) and **[`sprintPlan.md`](./sprintPlan.md)** (ISSUE-000/001/002) for the repo skeleton, pnpm workspace layout, and Docker Compose setup.

---

## The Default Rule: Open a Pull Request

**If you implemented something, open a PR. Full stop.**

Every issue, feature, fix, and refactor goes through a pull request — not directly to `main`.

Direct pushes to `main` are the **exception**. See Section 1 for the narrow cases where it is allowed.

---

## 1. Branch & Push Policy

### Pull Request Required

A pull request is **mandatory** for:

- Any implementation tied to a GitHub issue
- New endpoints, services, entities, or repository methods
- Changes to existing business logic or validation rules
- Database schema changes (new columns, tables, constraints, Flyway migrations)
- Any modification to `SecurityConfig`, `GlobalExceptionHandler`, or `application.properties`
- Frontend page or component additions
- Anything that touches a module owned by another team member (see module ownership in `sprintPlan.md`)

---

## 2. Pull Request Rules

### Opening a PR

- Link the PR to its issue: `Resolves #<issue-number>` in the PR body.
- Write a short description of what was implemented and any notable decisions made.
- Branch naming: `feature/issue-<number>-<short-description>`
- Request a review from at least one of the other two teammates when you open the PR.

### Review Requirements

- **Authors must not approve their own pull requests** — no exceptions.
- Any team member may review and leave comments.
- **A PR requires at least 1 approving review before it can be merged.** There is no team lead gate — the team is 3 people and every developer's approval carries equal weight.
- Once the PR has 1 approval and zero unresolved comments, **the author merges it** (or the approving reviewer, if the author is unavailable).

### Review Criteria

Reviewers must check all of the following before approving:

| Area | What to verify |
|------|---------------|
| **Correctness** | Does the implementation do what the linked issue describes? |
| **API contract** | Does it match `docs/apiEndpoints.md` — paths, methods, request/response shapes, status codes? |
| **Business rules** | Does it follow the process defined in `docs/businessProcessMapping.md`? No undocumented deviations. |
| **Security** | Is the endpoint's role guard wired in `SecurityConfig` (RBAC per `docs/apiEndpoints.md` role column)? No plaintext secrets in code or logs. |
| **Error handling** | Are failure cases handled explicitly? No silent exception swallowing. |
| **Tests** | Are there tests for new service methods and endpoints? Do existing tests still pass? |
| **Build** | Does the PR compile cleanly? No introduced compile errors or broken imports (backend Maven build, frontend pnpm build + lint). |

### Review Etiquette

- Leave specific, actionable comments.
- Mark suggestions clearly: `nit:` or `suggestion:` for non-blocking feedback.
- The author is responsible for responding to all comments before requesting re-review.
- Resolved threads should be marked resolved by the reviewer who raised them, not the author.

---

## 3. Commit Message Convention

| Prefix | When to use |
|--------|-------------|
| `feat:` | New feature or endpoint |
| `fix:` | Bug fix |
| `refactor:` | Code restructure with no behavior change |
| `chore:` | Config, dependency, or tooling change |
| `docs:` | Documentation only |
| `test:` | Tests only |
| `QA:` | Postman collections, test data, QA scripts |

Example: `feat: implement chatbot conversation history endpoint (#42)`

---

## 4. Code Quality Expectations

- Every new service method must handle failure cases explicitly — no silent exception swallowing.
- Established architectural decisions (module boundaries, RAG pipeline design, etc.) are finalized. Do not deviate without raising it with the other two developers first — a quick team sync is enough, no separate approval role needed.
- Do not add endpoints to controllers without a corresponding entry in `docs/apiEndpoints.md`.
- Do not introduce new dependencies without the team agreeing on it together.

---

## 5. What Blocks a Merge

A PR will not be merged if any of the following are true:

- Zero approving reviews from another teammate
- Any unresolved review comments
- The author has not responded to feedback
- The implementation deviates from `docs/apiEndpoints.md` without justification
- The PR breaks the build or introduces a compile error
- `SecurityConfig` not updated for newly added protected endpoints

---
