# CampusNest — Feature Backlog

Features we have agreed to add **later**, outside the current phase plan (see [PROJECT-PLAN.md](PROJECT-PLAN.md)).
Planned phase work (listings, scoring, roommates, etc.) is not listed here.

**How to use this list:** add new ideas at the bottom with status **Pending**. When we start one, change it to
**In progress**, and when it is done and tested, mark it **Done** and record it in [STATUS.md](STATUS.md).

| ID | Feature | Status | Why later | Notes / prerequisites |
|---|---|---|---|---|
| B1 | **Sign up / log in with Google** | Pending | Not a faculty requirement; needs internet during demos; the review is in about a week | Free (no credit card for basic sign-in). A team member creates a Google Cloud project + OAuth client ID/secret and puts them in `.env`. Needs: role choice (student / provider) after first Google login, linking to an existing account with the same email, nullable password for Google-only accounts (new migration). Testing mode allows 100 manually added test users and shows an "unverified app" warning. Estimate: ~1 day incl. tests. Suggested timing: after the review, with Phase 8 trust work (Google confirms the email address). |
| B2 | Change password (while logged in) | Pending | Not needed for the demo | Requires current password; end other sessions after change. |
| B3 | Forgot password / reset by email | Pending | Needs a free email-sending service | Must stay free; time-limited single-use reset tokens. Related to tech debt T11. |
| B4 | Email address verification | Pending | Needs a free email-sending service | Would also address T10 (registration revealing existing emails). Comes free for Google sign-in users (B1). |
