# Design: Splash Screen + Auth Persistence

**Date:** 2026-06-26
**Status:** Approved

## Goal

Khi user đã đăng nhập, mở lại app sẽ thấy splash logo ~1.5 giây rồi vào thẳng MainActivity — không cần đăng nhập lại.

## Flow

```
App mở → SplashActivity (launcher)
    └── Hiện logo ~1.5 giây
    └── Check Firebase Auth:
            ├── currentUser != null  →  MainActivity
            ├── currentUser == null + language_select_completed=true  →  AuthScreen
            └── currentUser == null + language_select_completed=false →  WelcomeActivity
```

## Files

| File | Action |
|------|--------|
| `presentation/splash/SplashActivity.kt` | CREATE |
| `res/layout/activity_splash.xml` | CREATE |
| `AndroidManifest.xml` | MODIFY — đổi launcher intent-filter sang SplashActivity |
| `WelcomeActivity.kt` | MODIFY — xóa đoạn comment `//if (isCompleted)` |

## SplashActivity Logic

- Delay 1500ms bằng `Handler(Looper.getMainLooper()).postDelayed`
- Đọc `FirebaseAuth.getInstance().currentUser`
- Đọc SharedPreferences key `language_select_completed`
- Route và gọi `finish()` để xóa khỏi back stack

## Layout activity_splash.xml

- Background: màu primary app (`#096332` hoặc theo theme)
- Căn giữa: ImageView logo `@drawable/logo`
- Không có button hay text thêm

## Notes

- Firebase Auth tự động lưu/làm mới token — không cần logic token thủ công
- `finish()` bắt buộc sau khi navigate để user không back về splash
- WelcomeActivity giữ nguyên, chỉ bỏ đoạn comment cũ
