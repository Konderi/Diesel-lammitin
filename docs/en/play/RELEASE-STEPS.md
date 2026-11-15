# Publishing on Google Play: steps

[Suomi](../../play/JULKAISU.md) · **English**

This guide complements the memo [../PLAY-RELEASE.md](../PLAY-RELEASE.md). Related files:

| File | Contents |
|---|---|
| [KAUPPA-TEKSTIT.md](../../play/KAUPPA-TEKSTIT.md) | Name, short and full description in English and Finnish |
| [FORMS.md](FORMS.md) | Data safety, SMS permissions declaration and other forms |
| [TESTAAJAT.md](../../play/TESTAAJAT.md) | Closed testing (12 testers / 14 days), invitation message fi/en |
| [privacy.html](../../play/privacy.html) | Privacy policy (fi/en). Published: https://konderi.github.io/privacy/heatcall/ (repo [Konderi/privacy](https://github.com/Konderi/privacy)) |
| [`assets/`](../../play/assets/) | Icon, feature graphics and screenshots |

## 1. Upload key

The Play build is signed with a dedicated upload key. Google Play then signs the distributed app with its own key (Play App Signing).

| File | Location | Under version control |
|---|---|---|
| Key | `~/.android/heatcall-upload.jks` | No |
| Passwords | `keystore.properties` (in the project root) | No (`.gitignore`) |

**Back up both to a password manager (e.g. 1Password) right away.** If the upload key is lost, it can be reset through Play Console support, but that takes time.

On a new machine, copy the key and `keystore.properties` into place and check that the `storeFile` path points to the key.

## 2. Building

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest bundlePlay
```

Output: `app/build/outputs/bundle/play/app-play.aab`. The build uses R8 minification, so the size is about 4 MB.

For each new Play release, increase `versionCode` (and `versionName`) in `app/build.gradle.kts`. The sideloadable APK (`assembleRelease`, debug key) and the Play version have different signatures. Users must uninstall the sideloaded version before installing the Play version.

## 3. Play Console

1. **Create app:** name *HeatCall*, default language English (en-US), app, **free**. See [Pricing](#pricing-free).
2. **App content:** fill in the forms as described in [FORMS.md](FORMS.md). Privacy policy URL: **https://konderi.github.io/privacy/heatcall/**
3. **Store listing:** texts from [KAUPPA-TEKSTIT.md](../../play/KAUPPA-TEKSTIT.md) and images from the [`assets/`](../../play/assets/) directory. Add Finnish as a translation.
4. **Testing → Internal testing:** create a release, upload `app-play.aab`, add yourself as a tester and install through Play. Test the status query, start, reply and marking as read.
5. **SMS permissions declaration:** submitted with the first release that uses SMS permissions. Review usually takes from a few days to a few weeks. If the declaration is rejected, see the fallback plan in [../PLAY-RELEASE.md](../PLAY-RELEASE.md#3-fallback-plan-if-the-exemption-is-not-granted).
6. **Closed testing** (mandatory for personal accounts created after November 2023): at least 12 testers for 14 days. After that you can apply for production access.
7. **Production:** create a release from the same AAB, choose countries (e.g. Finland + EU) and publish.

## Pricing: free

HeatCall is free and open source (https://github.com/Konderi/HeatCall). It has no ads and no in-app purchases.

- **No payments profile:** publishing a free app requires no merchant account and no bank details.
- **No promo codes:** testers install the app directly through the opt-in link.
- **Final choice:** a free app cannot later be changed to paid.
- **Forms:** *Contains ads* = No. The store listing shows no in-app purchases.

The store description mentions the requirements (EasyStart Call unit and SIM card), so users know before installing what the app needs. This is already included in [KAUPPA-TEKSTIT.md](../../play/KAUPPA-TEKSTIT.md).

## 4. Checklist before submitting

- [ ] App created as free
- [ ] Source code repo public: https://github.com/Konderi/HeatCall
- [x] Privacy policy published: https://konderi.github.io/privacy/heatcall/ (contact address heatcall@konderi.fi)
- [ ] Alias heatcall@konderi.fi works (send a test message)
- [ ] Upload key and `keystore.properties` backed up
- [ ] `versionCode` increased from the previous Play release
- [ ] AAB tested in internal testing with a real phone and heater
- [ ] Data safety, content rating, target audience and SMS declaration filled in
- [ ] Store listing: icon, feature graphic, at least 2 screenshots (en and fi)
