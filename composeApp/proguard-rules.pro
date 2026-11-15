# 라이브러리가 함께 주는 규칙(consumer rules)으로 충분하면 여기에 더하지 않는다.

# Play 리뷰 ktx가 컴파일 때만 쓰는 주석을 가리킴. 실행에는 없어도 됨(R8 missing_rules)
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
