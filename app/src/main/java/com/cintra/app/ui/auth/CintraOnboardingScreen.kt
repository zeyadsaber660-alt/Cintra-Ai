package com.cintra.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.R
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraThemePalette

enum class AuthMode(val labelAr: String, val labelEn: String) {
    GUEST("ضيف", "Guest"),
    GOOGLE("حساب Google", "Google Account"),
    EMAIL("البريد الإلكتروني", "Email Account"),
}

/**
 * Clean onboarding / sign-in choice screen allowing the user to pick
 * Guest, Google, or Email mode before entering Cintra (or when switching mode).
 */
@Composable
fun CintraOnboardingScreen(
    isArabic: Boolean,
    onContinue: (mode: AuthMode, displayName: String, email: String) -> Unit,
) {
    val palette = CintraThemePalette
    var showEmailForm by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.cintra_logo),
                contentDescription = "Cintra",
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )

            Text(
                text = if (isArabic) "مرحباً بك في Cintra" else "Welcome to Cintra",
                color = palette.textPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = if (isArabic) "اختر طريقة الدخول التي تناسبك للبدء" else "Choose how you would like to continue",
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            if (!showEmailForm) {
                // 1. Continue with Google
                AuthButton(
                    title = if (isArabic) "المتابعة بحساب Google" else "Continue with Google",
                    primary = true,
                    onClick = {
                        onContinue(AuthMode.GOOGLE, "Zeyad", "google.user@gmail.com")
                    },
                )

                // 2. Continue with Email
                AuthButton(
                    title = if (isArabic) "المتابعة بالبريد الإلكتروني" else "Continue with Email",
                    primary = false,
                    onClick = { showEmailForm = true },
                )

                // 3. Continue as Guest
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onContinue(AuthMode.GUEST, if (isArabic) "ضيف" else "Guest", "") }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isArabic) "المتابعة كضيف بدون تسجيل" else "Continue as Guest",
                        color = palette.accent,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(palette.surface)
                        .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(24.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (isArabic) "الدخول بالبريد الإلكتروني" else "Sign in with Email",
                        color = palette.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    AuthField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = if (isArabic) "الاسم (مثلاً: زياد)" else "Your name",
                    )
                    AuthField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = "name@example.com",
                    )
                    AuthButton(
                        title = if (isArabic) "دخول" else "Continue",
                        primary = true,
                        onClick = {
                            val finalName = nameInput.trim().ifEmpty { "User" }
                            onContinue(AuthMode.EMAIL, finalName, emailInput.trim())
                        },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEmailForm = false }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isArabic) "رجوع للخيارات" else "Back",
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthButton(
    title: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val palette = CintraThemePalette
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (primary) palette.accentSolid else palette.surface)
            .border(0.85.dp, if (primary) palette.accentSolid else palette.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = if (primary) palette.accentSolidText else palette.textPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val palette = CintraThemePalette
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = palette.textPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(palette.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface2)
            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = palette.textMuted, fontSize = 13.5.sp)
            inner()
        },
    )
}
