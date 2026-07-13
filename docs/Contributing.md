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

- Before opening a PR, create a GitHub issue for the backlog item first (from `issue.md` / `issuePhase2.md`) and **put the backlog ID in the issue title**, e.g. `[A-3] POST /chatbot/messages — Yazılı Soru-Cevap Akışı`. GitHub assigns its own issue number (`#14`); the backlog ID (`A-3`) is not the same thing and won't match it — the title is what keeps them linked.
- Link the PR to its issue: `Resolves #<issue-number>` in the PR body, and mention the backlog ID too, e.g. `Resolves #14 (A-3)`.
- Write a short description of what was implemented and any notable decisions made.
- Branch naming: `feature/issue-<number>-<short-description>`, e.g. `feature/issue-14-a3-chatbot-messages` — use the GitHub issue number, and fold the backlog ID into the description part so both are visible at a glance.
- Request a review from at least one of the other two teammates when you open the PR.

### Review Requirements

- **Authors must not approve their own pull requests** — no exceptions.
- Any team member may review and leave comments.
- **A PR requires at least 1 approving review before it can be merged.** There is no team lead gate — the team is 3 people and every developer's approval carries equal weight.
- Once the PR has 1 approval and zero unresolved comments, **the author merges it** (or the approving reviewer, if the author is unavailable).

### Review Criteria

Reviewers must check all of the following before approving:

| Area               | What to verify                                                                                                                                                                                       |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Correctness**    | Does the implementation do what the linked issue describes?                                                                                                                                          |
| **API contract**   | Does it match `docs/apiEndpoints.md` — paths, methods, request/response shapes, status codes?                                                                                                        |
| **Business rules** | Does it follow the process defined in `docs/businessProcessMapping.md`? No undocumented deviations.                                                                                                  |
| **Security**       | Is the endpoint's role guard wired in `SecurityConfig` (RBAC per `docs/apiEndpoints.md` role column)? No plaintext secrets in code or logs.                                                          |
| **Error handling** | Are failure cases handled explicitly? No silent exception swallowing.                                                                                                                                |
| **Tests**          | Existing tests still pass. New tests are only _required_ for what's listed under "Minimum required tests" in Section 3 — don't block a PR over missing tests for a plain CRUD form or layout change. |
| **Build**          | Does the PR compile cleanly? No introduced compile errors or broken imports (backend Maven build, frontend pnpm build + lint).                                                                       |

### Review Etiquette

- Leave specific, actionable comments.
- Mark suggestions clearly: `nit:` or `suggestion:` for non-blocking feedback.
- The author is responsible for responding to all comments before requesting re-review.
- Resolved threads should be marked resolved by the reviewer who raised them, not the author.

---

## 3. Testing Convention

We're not saving testing for a cleanup week at the end. Every week of the backlog carries its own small test issue per developer (`A-T1`, `B-T1`, `C-T1`, ... — one per week, see `issue.md` / `issuePhase2.md`) that covers the tests for that week's feature issues. Test as you go, not all at once in Week 4.

### Minimum required tests

Not everything needs a test to merge — for a 4-week MVP, full coverage isn't the goal. What **does** need at least one test:

- RBAC/security-guarded endpoints — a role that should be rejected actually gets rejected (e.g. `employee` → 403 on `/admin/*`)
- Business-critical invariants called out in the issue itself (e.g. FR-43 anonymity, FR-64 single source of truth for schedules, RAG "don't hallucinate" fallback)
- Any bug fix — add a regression test that fails without the fix

What does **not** need one to merge: plain admin CRUD forms, layout/styling changes, stretch/buffer issues. A test there is welcome but never blocks review.

---

## 4. Commit Message Convention

| Prefix      | When to use                                |
| ----------- | ------------------------------------------ |
| `feat:`     | New feature or endpoint                    |
| `fix:`      | Bug fix                                    |
| `refactor:` | Code restructure with no behavior change   |
| `chore:`    | Config, dependency, or tooling change      |
| `docs:`     | Documentation only                         |
| `test:`     | Tests only                                 |
| `QA:`       | Postman collections, test data, QA scripts |

Example: `feat: implement chatbot conversation history endpoint (#42)`

---

## 5. Code Quality Expectations

- Every new service method must handle failure cases explicitly — no silent exception swallowing.
- Established architectural decisions (module boundaries, RAG pipeline design, etc.) are finalized. Do not deviate without raising it with the other two developers first — a quick team sync is enough, no separate approval role needed.
- Do not add endpoints to controllers without a corresponding entry in `docs/apiEndpoints.md`.
- Do not introduce new dependencies without the team agreeing on it together.

---

## 6. What Blocks a Merge

A PR will not be merged if any of the following are true:

- Zero approving reviews from another teammate
- Any unresolved review comments
- The author has not responded to feedback
- The implementation deviates from `docs/apiEndpoints.md` without justification
- The PR breaks the build or introduces a compile error
- `SecurityConfig` not updated for newly added protected endpoints

---
