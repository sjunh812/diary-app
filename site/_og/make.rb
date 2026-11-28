# 링크 미리보기 그림(1200×630)을 언어별로 만듦. 사이트 문구·글꼴·캡처를 그대로 씀
# 쓰는 법: (site 폴더에서) sh build.sh && ruby _og/make.rb
require "yaml"
require "fileutils"

site = File.expand_path("..", __dir__)
out = File.join(site, "_site", "_og")
FileUtils.mkdir_p(out)
chrome = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
port = 8799
plain = ->(html, ja) { html.gsub(/<br>/, ja ? "" : " ").gsub(/<[^>]+>/, "") }
langs = YAML.load_file(File.join(site, "_data", "i18n.yml")).keys

YAML.load_file(File.join(site, "_data", "i18n.yml")).each do |lang, t|
  ja = lang == "ja"
  # 사이트와 같은 글꼴: 창원체(한국어·영어), Pretendard(유럽 네 언어), 시스템 글꼴(일본어·중국어)
  font = { "round" => '"Changwon Round",sans-serif', "sans" => '"Pretendard Latin",sans-serif' }.fetch(t["font"]) { lang == "zh-hant" ? '"PingFang TC","Noto Sans TC",sans-serif' : '"Hiragino Sans","Noto Sans JP",sans-serif' }
  # 굵은 글은 사이트와 같이 창원체만 테두리로 두껍게. 나머지는 글꼴의 굵기를 씀
  bold = t["font"] == "round" ? "-webkit-text-stroke:.034em currentColor" : "font-weight:700"
  File.write(File.join(out, "#{lang}.html"), <<~HTML)
    <!doctype html><meta charset="utf-8">
    <style>
    @font-face{font-family:"Changwon Round";src:url(/assets/fonts/changwon-round.woff2) format("woff2")}
    @font-face{font-family:"Pretendard Latin";src:url(/assets/fonts/pretendard-latin-semibold.woff2) format("woff2");font-weight:700}
    @font-face{font-family:"Pretendard Latin";src:url(/assets/fonts/pretendard-latin-regular.woff2) format("woff2");font-weight:400}
    *{box-sizing:border-box}
    body{margin:0;width:1200px;height:630px;overflow:hidden;position:relative;font-family:#{font};color:#1A1C20;word-break:keep-all;
      background:linear-gradient(135deg,#F3EFFF 0%,#FFFFFF 55%,#F6F3FF 100%)}
    .text{position:absolute;left:80px;top:150px;width:560px}
    .icon{width:96px;height:96px;border-radius:24px;box-shadow:0 8px 24px rgba(101,80,185,.16)}
    h1{margin:36px 0 0;font-size:#{t["font"] == "sans" ? 64 : 76}px;line-height:1.15;font-weight:#{t["font"] == "round" ? 400 : 800};letter-spacing:-.02em;#{bold}}
    p{margin:20px 0 0;font-size:36px;line-height:1.4;color:#555D6D}
    .phone{position:absolute;width:270px;padding:7px;border-radius:42px;background:#E9EAEE;box-shadow:0 24px 56px rgba(26,28,32,.16)}
    .phone img{display:block;width:100%;border-radius:35px}
    .back{left:650px;top:150px}
    .front{left:880px;top:70px}
    </style>
    <div class="text"><img class="icon" src="/assets/icon.png" alt=""><h1>#{t["name"]}</h1><p>#{plain.(t["hero"], ja || lang == "zh-hant")}</p></div>
    <div class="phone back"><img src="#{t["screens"]}write.jpg" alt=""></div>
    <div class="phone front"><img src="#{t["screens"]}list.jpg" alt=""></div>
  HTML
end

server = spawn("python3", "-m", "http.server", port.to_s, "--bind", "127.0.0.1", chdir: File.join(site, "_site"), out: File::NULL, err: File::NULL)
sleep 1
begin
  langs.each do |lang|
    png = File.join(out, "#{lang}.png")
    system(chrome, "--headless=new", "--hide-scrollbars", "--force-device-scale-factor=1", "--user-data-dir=#{out}/chrome",
           "--window-size=1200,630", "--virtual-time-budget=3000", "--screenshot=#{png}", "http://127.0.0.1:#{port}/_og/#{lang}.html",
           err: File::NULL, exception: true)
    system("sips", "-s", "format", "jpeg", "-s", "formatOptions", "86", png, "--out", File.join(site, "assets", "og-#{lang}.jpg"),
           out: File::NULL, exception: true)
  end
ensure
  Process.kill("TERM", server)
end
