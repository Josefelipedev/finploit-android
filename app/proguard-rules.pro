# O Retrofit usa GsonConverterFactory, que serializa por reflexão sobre os nomes
# dos campos: qualquer classe que vá para o corpo de um pedido tem de manter os
# nomes originais. Além de `data.dto`, há corpos declarados ao lado dos
# interfaces em `data.api` (EnrichRequest, BatchToggleRequest) — sem esta regra o
# R8 renomeia `items` para `a` e o servidor recebe um corpo sem os campos.
-keep class com.finploit.android.data.dto.** { *; }
-keep class com.finploit.android.data.api.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
