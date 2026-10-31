# My Music Player — Android starter project

यह एक शुरुआती Offline Music Player Android Studio प्रोजेक्ट है।

## क्या करता है
- फोन में मौजूद Music files को MediaStore से पढ़ता है
- गानों की सूची दिखाता है
- गाना चुनकर Play/Pause/Next/Previous करता है
- Login या इंटरनेट की जरूरत नहीं

## जरूरी बातें
- यह starter project है; अभी background playback notification, seek bar, favorites, playlists,
  album art और audio focus handling शामिल नहीं हैं।
- Android Studio में खोलकर Gradle Sync करें और फोन/एम्युलेटर पर टेस्ट करें।
- Android 13+ पर ऐप audio permission मांगेगा।
- यह प्रोजेक्ट किसी copyrighted गाने को शामिल नहीं करता; यह केवल डिवाइस पर मौजूद audio files चलाता है।

## APK बनाना
Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s)

## AAB बनाना
Android Studio → Build → Generate Signed Bundle / APK → Android App Bundle → Create/select keystore → Release → Finish

Release keystore और उसके passwords सुरक्षित रखें। Play Store पर update देने के लिए वही signing identity जरूरी हो सकती है।

## Build environment
JDK 17 और Android SDK Platform 35. Android Studio का हाल का stable version इस्तेमाल करें।


## केवल मोबाइल से Build (GitHub Actions)
1. ZIP को फोन में डाउनलोड करके Extract करें।
2. GitHub पर नया repository बनाएं (Public या Private)।
3. Extract किए हुए प्रोजेक्ट के सभी files/folders repository में upload करें। `.github/workflows/android.yml` भी शामिल करें।
4. GitHub repository में Actions tab खोलें। `Build Android app` workflow चुनकर `Run workflow` दबाएं (या `main` पर push के बाद अपने-आप चलेगा)।
5. सफल run के बाद नीचे Artifacts से `MyMusicPlayer-build` डाउनलोड करें और ZIP extract करें।
6. `app-debug.apk` फोन में install करके टेस्ट करें। फोन पूछे तो केवल भरोसेमंद अपनी बनाई APK के लिए install permission दें।

नोट: इस workflow से बनने वाला release AAB अभी unsigned है; Play Store पर प्रकाशित करने के लिए सुरक्षित upload keystore से sign करना होगा। Keystore/password को source code में कभी upload न करें।
