"""Generate OpenSSF autofill links from reviewed evidence, without credentials.

Run python3 scripts/best-practices.py, open the printed HTML file, and save
each proposed section in your authenticated browser. Unknown answers are omitted.
The repository JSON also supplies proposals once merged into the default branch.
See https://github.com/ossf/best-practices-badge/blob/main/docs/automation-proposals.md
"""
import argparse
from html import escape
import json
from pathlib import Path
from urllib.parse import urlencode

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "build/best-practices.html")
    args = parser.parse_args()
    answers = json.loads((ROOT / ".bestpractices.json").read_text())
    base = "https://www.bestpractices.dev/en/projects/16413/passing/edit?"
    groups, current, unknown = [], {}, []
    metadata = {"name", "description", "license", "implementation_languages"}
    for key, value in answers.items():
        if key in metadata:
            pair = {key: value}
        elif key.endswith("_status"):
            criterion = key.removesuffix("_status")
            if value == "?":
                unknown.append(criterion)
                continue
            if value not in {"Met", "Unmet", "N/A"}:
                raise ValueError(f"Invalid status: {key}={value}")
            justification = criterion + "_justification"
            pair = {key: value, justification: answers[justification]}
        elif key.endswith("_justification"):
            continue
        else:
            raise ValueError(f"Unexpected field: {key}")
        # Keep status and evidence together; bound URLs for proxies/browsers.
        if len(base + urlencode(current | pair)) > 6000:
            if not current or len(base + urlencode(pair)) > 6000:
                raise ValueError(f"Proposal too long: {key}")
            groups.append(current)
            current = {}
        current.update(pair)
    if current:
        groups.append(current)
    links = [base + urlencode(group) for group in groups]
    content = ['<!doctype html><html lang="en"><meta charset="utf-8">',
               '<title>Myna OpenSSF evidence autofill</title>',
               '<h1>Myna OpenSSF evidence autofill</h1>',
               '<p>Sign in to Best Practices with the account that owns project 16413. '
               'Open each link in order, review the proposals, then Save and exit before '
               'opening the next link. Existing answers are not overridden. '
               'These links do not award a badge or submit answers automatically.</p><ol>']
    for index, link in enumerate(links, 1):
        content.append(f'<li><a href="{escape(link, quote=True)}">Fill part {index} of {len(links)}</a></li>')
    content.append('</ol><h2>Answers still requiring review</h2><ul>')
    for criterion in unknown:
        content.append(f'<li>{escape(criterion)}: {escape(answers[criterion + "_justification"])}</li>')
    content.append('</ul><p><a href="https://www.bestpractices.dev/en/projects/16413">View saved progress</a></p></html>')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text('\n'.join(content) + '\n')
    print(args.output.resolve())
    print(f'{len(links)} autofill links; {len(unknown)} unanswered criteria')


if __name__ == "__main__":
    main()
