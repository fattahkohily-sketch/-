# Bluetooth Chess App

ملف المشروع تم تجهيزـه في مستودع GitHub الخاص بك.

## الوصف
- تطبيق Android Native باستخدام Kotlin
- لعبة شطرنج تعمل على لوحين كاملين
- اتصال غير متصل بالإنترنت عبر البلوتوث بين جهازين
- واجهة بسيطة قابلة للتوسعة
- بداية جيدة لتطويرها لاحقًا إلى نسخة أكثر احترافية

## الملفات الأساسية
- `app/src/main/java/com/example/bluetoothchess/MainActivity.kt`
- `app/src/main/java/com/example/bluetoothchess/ChessGame.kt`
- `app/src/main/java/com/example/bluetoothchess/bluetooth/BluetoothController.kt`
- `app/src/main/res/layout/activity_main.xml`

## ملاحظات
هذا إصدار أولي يعمل كـ "base prototype". له ثلاث مكونات رئيسية:
1. اللوحة والقطع
2. منطق حركة القطع
3. اتصال Bluetooth بين الجهازين

## تشغيل المشروع
افتح المشروع في Android Studio ثم:
- File > Open
- اختر مجلد المشروع
- انتظر تنزيل الـ Gradle
- شغّل التطبيق على جهاز فعلي أو محاكي

## ملاحظة مهمة
يتطلب البلوتوث صلاحيات Android، ويمكن أن يختلف الأداء حسب الجهاز ونظام التشغيل. في Android 12+ تحتاج صلاحيات Bluetooth Scan و Connect.
