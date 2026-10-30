LIA AI - Starter Android Project

এই Starter-এ আছে:
- Gemini API key entry screen
- Gemini text chat (REST API)
- Android SpeechRecognizer দিয়ে একবারের voice input
- Android TextToSpeech voice reply
- Purple dark UI

গুরুত্বপূর্ণ:
- এটি এখনো Always-On Wake Word নয়।
- SpeechRecognizer button চাপলে একবার শুনবে।
- Android TTS-এর female voice ফোনের installed voice engine-এর ওপর নির্ভর করে।
- API key private preferences-এ রাখা হয়েছে; production release-এর আগে Android Keystore encryption ও server-side key protection বিবেচনা করুন।
- API endpoint/model availability বদলাতে পারে; প্রয়োজন হলে model name update করতে হবে।
- Phone control, WhatsApp automation, Accessibility service এবং 3D PMX/VRM avatar এখনো যোগ করা হয়নি.

Build:
GitHub repository-তে project upload করুন. Actions tab থেকে "Build LIA AI APK" চালান.
APK artifact download করে Android-এ install করুন.
