plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 sisältää Kotlinin; tämä vain lukitsee KGP-version samaksi kuin Compose-kääntäjä.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
