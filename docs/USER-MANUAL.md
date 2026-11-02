# CampusNest — User Manual

CampusNest helps students find accommodation near their college in Mumbai, compare costs and commutes, and find
compatible roommates, while owners list rooms and administrators keep listings trustworthy.

> **Important:** scores, travel times and transport costs are **estimates** to help you compare. Verification means
> CampusNest checked an owner's identity or reviewed a listing; it does **not** guarantee a property's condition or
> safety. Always visit before paying any money.

## For students

### Getting started
1. **Sign up** → choose "I'm a student" → enter your name, email and a password (at least 8 characters).
2. Open **Account** → choose your **college** (needed for commute estimates), course and year.
   Gender is optional: it is used only to match women-only / men-only accommodation and roommate preferences,
   and is never shown to other users.

### Finding accommodation
- **Find accommodation:** filter by maximum rent, distance, room type, who the place is for, must-have amenities and
  "only with free beds". Switch between **List** and **Map**. Distances are straight-line.
- Open a listing to see rooms and rent, extra charges, amenities (Yes / No / Not specified), photos, the map,
  when availability was last confirmed, student reviews, and whether the owner's identity was checked.

### Getting recommendations
1. **Recommended** → **Set my preferences**: maximum monthly budget (including compulsory charges), longest
   commute, how you travel, room types in order of preference, and amenities (must have / nice to have).
2. Listings are ranked by a **suitability score** out of 100. Press **Why this score?** to see the five parts:
   budget 30%, commute 25%, room type 15%, amenities 15%, trust 15%, each with a reason.
3. Listings with no free beds, not open to your gender, or more than 30% over your budget are left out; the page
   tells you how many and why.

### Comparing cost and commute
1. Press **+ Compare** on 2–4 listings (search, recommendations or listing pages), then **Compare cost and commute**.
2. The table shows rent, compulsory and optional charges, deposit, and for each way of travelling the estimated
   time and monthly cost. Change **Assume travel by** or **College days per month** to see different totals.
3. "Incomplete" means part of the cost is unknown (for example, local-train pass prices that have not been entered);
   CampusNest never guesses.

### Saving and messaging
- **☆ Save** a listing to find it later under **Saved**.
- **Message the owner** from a listing page. Your email and phone are hidden unless you tick
  "Share my email and phone". Conversations are under **Messages**; only you and the owner can read them.

### Reviews and reports
- After you have contacted an owner, you can **review** that listing (1–5 stars and a comment). Reviews are marked
  "stay not verified". Please don't include phone numbers.
- Use **Report this listing**, **Report review** or **Report** (on a roommate) if something looks fake, wrong,
  unsafe or abusive. Reports go to administrators only.

### Finding a roommate
1. **Roommates** → **Create my roommate profile**: budget range, preferred areas, sleep time, tidiness, guests,
   smoking (and whether you can live with a smoker), study environment, optional food habits, how many roommates,
   and whether you want only your own gender.
2. Only students who also made their profile visible can see you, and you can see others only while your profile
   is visible. They see your **first name and last initial**, course, year and habits, never your email, phone,
   gender or exact budget.
3. Each match shows a **compatibility %**, what you have in common and the differences to talk about. A number
   cannot guarantee you will get along: talk first, and meet in a public place.
4. **Ask to connect**. When the other student accepts, both of you see each other's name, email and phone under
   **Connections**. You can **Block** anyone at any time.

## For accommodation providers (owners)

### Creating a listing
1. Sign up as **"I'm an accommodation provider"**. In **Account**, choose your provider type and business name.
2. **My listings** → enter a title → **Create listing**.
3. Fill in: property type, who can stay, description (30+ characters), address, locality, PIN code; **click the map**
   to mark the building (or type the coordinates); each **room type** with rent, deposit, total beds and free beds;
   **extra charges** (tick "every tenant must pay this" for compulsory ones); and answer **Yes/No** for every
   amenity marked *.
4. Add **photos** (JPEG/PNG, up to 5 MB, max 10). Location data inside photos is removed automatically.
5. The **Submit for review** box lists anything still missing. Press **Save and submit for review**.

### After submitting
- **Under review:** you can't edit until an administrator decides.
- **Changes requested:** read the message at the top, fix the listing and submit again.
- **Published:** students can see it. Update **free beds** in the Availability box and press **Confirm availability**
  regularly; listings not confirmed for 30 days are shown as possibly out of date.
- Changing the title, description, address, location, type, who can stay, amenities or photos sends the listing back
  for review. Rent, charges and bed counts can be changed at any time.
- **Rejected** listings are locked. **Suspended** listings are hidden from students; the reason is shown to you.

### Messages and verification
- Student questions appear under **Messages**. You can choose to share your contact details in a reply.
- On **My listings**, press **Request identity verification**. An administrator will contact you (for example by
  phone). If confirmed, your listings show "Owner identity checked". No ID documents are stored.

## For administrators

There is no public admin sign-up: the first admin account is created from the server's `.env` settings.

- **Listing review:** listings waiting for review, oldest first. Open one to see everything the student will see plus
  the owner's contact details and history. **Approve and publish**, **Request changes**, or **Reject** (a reason is
  required and shown to the owner). Published listings can be **Suspended**; suspended ones **Reinstated**
  (they return as "full" until the owner confirms availability).
- **Reports:** uphold (with a note) or dismiss. An upheld listing report lowers that listing's trust score for
  90 days. For review reports you can also **Hide review**. Suspending a listing or user is a separate action.
- **Owners:** verification requests. Check the owner (call, video call, visit, or look at a document **without
  storing it**), choose the method, write what you checked, then **Mark identity checked** or **Reject**.
  Verified owners can be **revoked**.
- **Users:** suspend (reason required) or reinstate students and owners. A suspended user is logged out immediately.
- **Fares:** the fare rules used for transport cost estimates. Add rules only from official or clearly cited sources,
  with the date they apply from (for example, local-train monthly pass prices from the RailOne app).
