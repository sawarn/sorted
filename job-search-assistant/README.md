# Workboard — Job Search Assistant

A small, local-first job search app. It searches the Greenhouse and Lever career boards you add, ranks listings against your saved profile keywords, prepares an editable application draft, and tracks application status.

## Run it

Requires Python 3. No packages or API keys are needed.

```sh
cd job-search-assistant
python3 app.py
```

Open <http://127.0.0.1:8765>. Stop the app with Ctrl+C.

The server listens on your computer only. Data is saved to `job_search.sqlite3` in this folder. That file is **not encrypted**. Stop the app and delete the file to erase the saved profile, resume text, boards, matches, and drafts.

## Add job boards

Add a company name, choose Greenhouse or Lever, and enter the board name from the company's career-site URL. For example, for `boards.greenhouse.io/acme`, the board name is `acme`. Search checks the public listings on each board you added.

## Current boundaries

- Matching is a transparent keyword estimate based on roles, skills, avoided terms, and location. It is not an assessment of your qualifications.
- Resume input accepts pasted text or `.txt`/`.md` files. PDF extraction is not included.
- Application details and a cover-letter draft are prepared for your review. The app opens the employer's application page, but it does not fill or submit that site's form.
- Search, ranking, profile data, and drafting run without sending your resume to an AI service. Career-board requests include only the selected public job-board name.
- Verify the job is still open, check every application answer, and submit on the employer's site yourself.
