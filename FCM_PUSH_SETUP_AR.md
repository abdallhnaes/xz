# تشغيل إشعارات الهاتف حتى لو كان التطبيق مغلقًا

هذه النسخة أضافت Firebase Cloud Messaging + Cloud Functions.

## حتى تصل الإشعارات والتطبيق مغلق

يجب تنفيذ خطوتين في Firebase:

### 1) Android app و google-services.json

من Firebase Console:

1. Project settings
2. Add app
3. اختر Android
4. اكتب Package name:

`com.zajel.exact`

5. حمّل ملف:

`google-services.json`

6. ضع الملف داخل المشروع في:

`app/google-services.json`

أنا وضعت ملفًا مبدئيًا، لكن الأفضل أن تستبدله بالملف الرسمي من Firebase حتى تعمل FCM بشكل مضمون.

### 2) نشر Cloud Functions

من جهازك ثبّت Firebase CLI ثم نفّذ داخل مجلد المشروع:

```bash
npm install -g firebase-tools
firebase login
firebase init functions
firebase deploy --only functions,database
```

أو إذا كان المشروع جاهزًا:

```bash
cd functions
npm install
cd ..
firebase deploy --only functions,database
```

## ماذا يحدث بعد ذلك؟

- عندما يعدل المدير فاتورة أو إعدادات، يتم إنشاء إشعار في Firebase.
- Cloud Function تلتقط الإشعار وترسله عبر FCM إلى أجهزة الموظف.
- عندما يرسل الموظف رسالة، تصل للمدير كإشعار هاتف.
- عندما يرد المدير، تصل للموظف كإشعار هاتف.
- الإشعار يصل حتى لو كان التطبيق مغلقًا، بشرط أن يكون الجهاز متصلًا بالإنترنت وأن تكون إشعارات التطبيق مسموحة.

## اسم التطبيق وأيقونته

تم تغيير الاسم إلى:

`زاجل سحابي`

وتم تغيير الأيقونة إلى شكل جديد بحرف Z وسحابة.
