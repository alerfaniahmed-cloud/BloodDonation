package com.ahmed.blooddonation

import com.google.firebase.auth.PhoneAuthProvider

// كائن مؤقت يحفظ رمز إعادة الإرسال ورقم الجوال أثناء عملية التحقق
object PhoneAuthHolder {
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    var phoneNumber: String? = null
}
