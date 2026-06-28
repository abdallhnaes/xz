# زاجل سحابي

هذه نسخة محدثة من تطبيق زاجل مع:

- تغيير اسم التطبيق إلى: زاجل سحابي.
- تغيير الأيقونة إلى أيقونة جديدة بحرف Z وسحابة.
- رمز المالك يطلب مرة واحدة فقط بعد تثبيت التطبيق.
- حساب مدير وحساب موظف.
- كل حساب يغير PIN الخاص به بنفسه.
- الموظف لا يستطيع التعديل.
- مزامنة عبر Firebase Realtime Database.
- إشعارات هاتف عبر Firebase Cloud Messaging.
- Cloud Functions لإرسال الإشعارات حتى لو كان التطبيق مغلقًا.

## مهم جدًا

حتى تعمل إشعارات الهاتف والتطبيق مغلق، اقرأ ملف:

`FCM_PUSH_SETUP_AR.md`

يجب وضع ملف `google-services.json` الرسمي من Firebase داخل:

`app/google-services.json`

ثم نشر Cloud Functions:

```bash
firebase deploy --only functions,database
```

## بناء APK عبر GitHub

ارفع المشروع على GitHub وشغّل:

`Build Zajel APK`

وسيخرج ملف:

`Zajel.apk`
