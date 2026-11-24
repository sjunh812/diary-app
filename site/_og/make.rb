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

YAML.load_file(File.join(site, "_data", "i18n.yml")).each do |lang, t|
  ja = lang == "ja"
  font = ja ? '"Hiragino Sans","Noto Sans JP",sans-serif' : '"Changwon Round",sans-serif'
  # 굵은 글은 사이트와 같이 테두리로 두껍게. 일본어는 시스템 글꼴의 굵기를 씀
  bold = ja ? "font-weight:700" : "-webkit-text-stroke:.034em currentColor"
  File.write(File.join(out, "#{lang}.html"), <<~HTML)
    <!doctype html><meta charset="utf-8">
    <style>
    @font-face{font-family:"Changwon Round";src:url(/assets/fonts/changwon-round.woff2) format("woff2")}
    *{box-sizing:border-box}
    body{margin:0;width:1200px;height:630px;overflow:hidden;position:relative;font-family:#{font};color:#1A1C20;word-break:keep-all;
      background:linear-gradient(135deg,#F3EFFF 0%,#FFFFFF 55%,#F6F3FF 100%)}
    .text{position:absolute;left:80px;top:150px;width:560px}
    .icon{width:96px;height:96px;border-radius:24px;box-shadow:0 8px 24px rgba(101,80,185,.16)}
    h1{margin:36px 0 0;font-size:76px;line-height:1.15;font-weight:#{ja ? 800 : 400};letter-spacing:-.02em;#{bold}}
    p{margin:20px 0 0;font-size:36px;line-height:1.4;color:#555D6D}
    .phone{position:absolute;width:270px;padding:7px;border-radius:42px;background:#E9EAEE;box-shadow:0 24px 56px rgba(26,28,32,.16)}
    .phone img{display:block;width:100%;border-radius:35px}
    .back{left:650px;top:150px}
    .front{left:880px;top:70px}
    </style>
    <div class="text"><img class="icon" src="/assets/icon.png" alt=""><h1>#{t["name"]}</h1><p>#{plain.(t["hero"], ja)}</p></div>
    <div class="phone back"><img src="#{t["screens"]}write.jpg" alt=""></div>
    <div class="phone front"><img src="#{t["screens"]}list.jpg" alt=""></div>
  HTML
end

server = spawn("python3", "-m", "http.server", port.to_s, "--bind", "127.0.0.1", chdir: File.join(site, "_site"), out: File::NULL, err: File::NULL)
sleep 1
begin
  %w[ko en ja].each do |lang|
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
