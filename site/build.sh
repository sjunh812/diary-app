#!/bin/sh
# 앱 웹사이트(sjunh812.pages.dev) 빌드와 배포
# 처리방침·약관은 docs/legal 원문에서 만든다
set -e
cd "$(dirname "$0")"
for n in privacy terms; do
  if [ "$n" = privacy ]; then t="개인정보처리방침"; else t="이용약관"; fi
  {
    printf -- "---\nlayout: default\ntitle: %s\nheading: 1일 1일기 %s\npermalink: /1day1diary/%s/\n---\n\n" "$t" "$t" "$n"
    # 줄바꿈이 필요한 인용 줄과 공고일 줄 끝에 공백 두 칸
    awk '{ if ((prev ~ /^> / && $0 ~ /^> /)) { lines[NR-1]=lines[NR-1] "  " } lines[NR]=$0; prev=$0 } END { for (i=1;i<=NR;i++) { l=lines[i]; if (l ~ /^공고일/) l=l "  "; print l } }' "../docs/legal/$n.md"
  } > "1day1diary/$n.md"
  # 영어 번역본. 한국어 원문이 우선
  if [ "$n" = privacy ]; then t="Privacy Policy"; else t="Terms of Use"; fi
  {
    printf -- "---\nlayout: default\nlang: en\ntitle: %s\nheading: 1 Day 1 Diary %s\npermalink: /en/1day1diary/%s/\n---\n\n" "$t" "$t" "$n"
    awk '{ if ((prev ~ /^> / && $0 ~ /^> /)) { lines[NR-1]=lines[NR-1] "  " } lines[NR]=$0; prev=$0 } END { for (i=1;i<=NR;i++) print lines[i] }' "../docs/legal/en/$n.md"
  } > "en/1day1diary/$n.md"
done
jekyll build
if [ "$1" = deploy ]; then
  npx -y wrangler@4 pages deploy _site --project-name=sjunh812 --branch="${2:-main}" --commit-dirty=true
fi
