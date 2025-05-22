<img src="https://github.com/niedev/RTranslator/blob/v2.00/images/logo_beta_cut.png" width="280">

RTranslator is an (<a href='https://github.com/niedev/RTranslator?tab=readme-ov-file#libraries-and-models'>almost</a>) open-source, free, and offline real-time translation app for Android.

Connect to someone who has the app, connect Bluetooth headphones, put the phone in your pocket and you can have a conversation as if the other person spoke your language.
<br /><br />

![Conversation mode](https://github.com/niedev/RTranslator/blob/v2.00/images/Conversation_image.png)
<br /><br />
![WalkieTalkie mode and Costs](https://github.com/niedev/RTranslator/blob/v2.00/images/TextTranslation_and_WalkieTalkie.png)
<br /><br />

<h3>Conversation mode</h3>

The Conversation mode is the main feature of RTranslator. In this mode, you can connect with another phone that uses this app. If the user accepts your connection request:

- When you talk, your phone (or the **Bluetooth headset**, if connected) will capture the audio.

- The audio captured will be converted into text and sent to the interlocutor's phone.

- The interlocutors' phone will translate the text received into his language.

- The interlocutors' phone will convert the translated text into audio and will reproduce it from its speaker (or by the Bluetooth headset of the interlocutor if connected to his phone). 

All this in both directions.

Each user can have more than one connected phone so that you can translate conversations between more than two people and in any combination.
<br /><br />

<h3>WalkieTalkie mode</h3>

If conversation mode is useful for having a long conversation with someone, this mode instead is designed for quick conversations, such as asking for information on the street or talking to a shop assistant.

This mode only translates conversations between two people, it doesn't work with Bluetooth headsets, and you have to talk in turns. It's not a real simultaneous translation, but it can work with **only one phone**.

In this mode, the smartphone microphone will listen in two languages (selectable in the same screen of the walkie talkie mode) simultaneously. <br />
The app will detect in which language the interlocutor is speaking, translate the audio into the other language, convert the text into audio, and then reproduce it from the phone speaker. When the TTS has finished, it will automatically resume listening.
<br /><br />

<h3>Text translation mode</h3>

This mode is just a classic text translator, but always useful.
<br /><br />

<h3>General</h3>

RTranslator uses <a href="https://ai.meta.com/research/no-language-left-behind/">Meta\'s NLLB</a> for translation and <a href="https://openai.com/index/whisper/">OpenAi\'s Whisper</a> for speech recognition, both are (<a href='https://github.com/niedev/RTranslator?tab=readme-ov-file#libraries-and-models'>almost</a>) open-source and state of the art AIs, have excellent quality and run directly on the phone, ensuring absolute privacy and the possibility of using RTranslator even offline without loss of quality.

Also, RTranslator works even in the background, with the phone on standby or when using other apps (only when you use Conversation or WalkieTalkie modes). However, some phones limit the power in the background so in that case it is better to avoid it and keep the app open with the screen on.
<br />

<a href="https://www.producthunt.com/posts/rtranslator-2?embed=true&utm_source=badge-featured&utm_medium=badge&utm_souce=badge-rtranslator&#0045;2" target="_blank"><img src="https://api.producthunt.com/widgets/embed-image/v1/featured.svg?post_id=487672&theme=light" alt="RTranslator - Open&#0045;source&#0032;and&#0032;offline&#0032;simultaneous&#0032;translator&#0032;for&#0032;Android | Product Hunt" style="width: 250px; height: 54px;" width="250" height="54" /></a>
<br /><br />

<h3>User Accounts</h3>

To enhance your experience and manage features like usage limits and subscriptions, RTranslator now supports user accounts.

- **Create an Account:** You can easily sign up using your email and password. Support for Google and GitHub sign-in is also available for quick access.
- **Benefits:**
    - Track and manage your subscription status.
    - Sync your microphone usage data.
    - (Future) Access personalized settings and features across devices.
- **Password Management:** If you forget your password, a "Forgot Password" option is available on the login screen to securely reset it via email.
<br /><br />

<h3>Subscription Model</h3>

RTranslator offers two tiers to suit your needs:

- **Free Tier:**
    - Enjoy core translation features with on-device processing.
    - Includes a daily microphone usage limit of up to 60 minutes for real-time voice input. This limit resets daily.
    - Text translation mode remains unlimited.

- **Premium Tier (Subscription-based):**
    - **Unlimited microphone usage:** Speak and translate without daily time restrictions.
    - Access to all current and future premium features.
    - **How to Subscribe:** You can subscribe to a monthly or yearly plan directly within the app through the "Subscription" page (if available, or via prompts when limits are reached). Purchases are securely handled by Google Play Billing.
    - **Manage Your Subscription:** Your subscription can be managed or cancelled at any time through your Google Play Store account settings.

This model helps support the ongoing development and maintenance of RTranslator while keeping core features accessible.
<br /><br />

<h3>Offline Functionality with User Accounts</h3>

RTranslator continues to prioritize offline functionality for its core translation capabilities:

- **AI Models On-Device:** Speech recognition (Whisper) and translation (NLLB) models run entirely on your device, ensuring privacy and offline availability once downloaded.
- **Account Authentication:**
    - Initial account sign-up and login require an internet connection.
    - Once logged in, your session is cached locally, allowing you to use the app offline for a period.
- **Subscription & Usage Sync:**
    - An internet connection is needed to make a new subscription purchase or to restore an existing one on a new device.
    *   Your subscription status and current microphone usage are cached locally. The app will attempt to sync this data periodically when online to ensure daily limits are correctly enforced and your subscription status is up-to-date. If you are offline for an extended period, the displayed remaining usage might not reflect the most recent synced data until you go online.
<br /><br />

<h3>Download</h3>

To install the app, download the latest version of the app apk file from https://github.com/niedev/RTranslator/releases/ and install it (ignore the other files, those will be downloaded automatically by the app on the first start).

<a href='https://github.com/niedev/RTranslator/releases'><img alt='Get it on GitHub' src='https://github.com/niedev/RTranslator/blob/v2.00/images/get_it_on_github.png' style="width: 180px; height: 58px;" /></a>

On the first launch, RTranslator will automatically download the models for translation and speech recognition (1.2GB) and once done you can start translating.

The initial download will get the models from GitHub, however <a href="https://www.reddit.com/r/China/comments/v8fv0p/why_is_github_so_slow_in_china_recently/">in some regions GitHub is very slow</a>, those who have problems of this kind can download the models separately from a computer (or in general in whatever way they prefer) and insert them manually into the app following <a href="https://github.com/niedev/RTranslator/blob/v2.00/Sideloading.md">this guide</a>.

If you have a GitHub account and want to be notified when a new release comes out you can do so by clicking, at the top of the page, on "Watch" -> "Custom" -> "Releases" -> "Apply".
<br /><br />

<h3>What's new in version 2.1</h3>

- **New GUI!** Designed by [Chiara Chindamo](https://www.linkedin.com/in/chiara-chindamo-946053234/).

- Added speak and copy buttons to the text translation mode.

- Added the option to manually control the mics in WalkieTalkie mode.

- Added the option to use low-quality languages.

- Fixed some bugs.

For the full list of changes see [here](https://github.com/niedev/RTranslator/releases/tag/2.1.0).
<br /><br />

<h3>Performance requirements</h3>

I have optimized the AI models a lot to minimize RAM consumption and execution time, despite this however to be able to use the app without the risk of crashing you need a phone with at least **6GB of RAM**, and to have a good enough execution time you need a phone with a fast enough CPU.

If you have a pretty crappy phone (or if you want maximum speed) you can always use <a href="https://github.com/niedev/RTranslator/tree/v1.00">version 1.0 of RTranslator</a> (but since it uses Google APIs it's not free and needs some initial setup).
<br /><br />


<h3>Supported languages</h3>

The languages supported are as follows:

Arabic, Bulgarian, Catalan, Chinese, Croatian, Czech, Danish, Dutch, English, Finnish, French, Galician,  German, Greek, Italian, Japanese, Korean, Macedonian, Polish, Portuguese, Romanian, Russian, Slovak, Spanish, Swedish, Tamil, Thai, Turkish, Ukrainian, Urdu, Vietnamese.
<br /><br />
If your language is not on the list, from version 2.1 of RTranslator you can go into the settings and enable "**Support low quality languages**" to add the following languages (which have lower quality for translation and speech recognition):

Afrikaans, Akan (only text), Amharic, Assamese, Bambara (only text), Bangla, Bashkir, Basque, Belarusian, Bosnian, Dzongkha (only text), Esperanto (only text), Estonian, Ewe (only text), Faroese, Fijian (only text), Georgian, Guarani (only text), Gujarati, Hausa, Hebrew, Hindi, Hungarian, Irish (only text), Javanese (only text), Kannada, Kashmiri (only text), Kazakh, Kikuyu (only text), Kinyarwanda (only text), Korean, Kyrgyz (only text), Lao, Limburghish (only text), Lingala, Lithuanian, Luxembourghish, Macedonian, Tagalog (only text), Tibetan.
<br /><br />


<h3>Text To Speech</h3>

To speak, RTranslator uses the system TTS of your phone, so the quality of the latter and the supported languages ​​depend on the system TTS of your phone.

The supported languages ​​seen above are all compatible with <a href="https://play.google.com/store/apps/details?id=com.google.android.tts&pcampaignid=web_share">Google TTS</a>, which is the recommended TTS (although you can use the TTS you want).

To change the system TTS (and therefore the TTS used by RTranslator), download the TTS you want to use from the Play Store, or from the source you prefer, and open RTranslator, then open its settings (top right) and, in the "Output" section, click on "Text to Speech", at this point the system settings will open in the section where you can select the preferred system TTS engine (among those installed), at this point, if you have changed the preferred engine, restart RTranslator to apply the changes.
<br /><br />

<h3>Privacy</h3>

Privacy remains a fundamental aspect of RTranslator. With the introduction of user accounts and subscriptions, here's how your data is handled:

- **Core Translation Data:** Speech input and translated text are processed on your device by local AI models (Whisper and NLLB) and are not sent to any external servers for translation purposes.
- **User Account Information:**
    - To create and manage your account, we store your email address and a secure Supabase User ID. OAuth sign-ins (Google, GitHub) are handled through their respective platforms, and RTranslator receives your email and a unique identifier.
    - Passwords are managed securely by Supabase authentication services.
- **Microphone Usage Data:**
    - For users on the Free Tier, the duration of microphone usage is tracked and associated with your account to manage daily limits. This usage data (total duration) is periodically synced with our Supabase backend.
- **Subscription Status:**
    - Your subscription status (e.g., active, expired, tier) and expiry date are associated with your Supabase User ID and stored on our backend to manage access to premium features.
    - Purchase tokens from Google Play Billing are sent to our backend (Supabase Edge Function) for one-time validation with Google's servers to confirm the subscription. We store a record of this validation.
- **Payment Information:** All payment processing for subscriptions is handled directly and securely by Google Play Billing. RTranslator does not see, collect, or store your credit card details or other sensitive payment information.
- **Data Storage:** Account information, usage metadata, and subscription status are stored using Supabase, a trusted third-party backend service.

**Important Note:** The existing <a href="https://github.com/niedev/RTranslator/blob/v2.00/privacy/Privacy_Policy_en.md" target="_blank" rel="noopener noreferrer">privacy policy document</a> needs to be updated to accurately reflect these new data handling practices for user accounts, usage tracking, and subscriptions. **Users are encouraged to review the updated policy once available.** We are committed to transparency and protecting your data.
<br /><br />

<h3>Libraries and models</h3>

RTranslator code is completely open-source, but some of the external libraries it uses have less permissive licenses, these are all the external libraries used by the app (with the indication of their license):

<a href="https://github.com/niedev/BluetoothCommunicator">BluetoothCommunicator</a> (open-source): Used for Bluetooth LE communication between devices.

<a href="https://github.com/niedev/GalleryImageSelector">GalleryImageSelector</a> (open-source): Used for selecting and cropping the profile image from the gallery.

[OnnxRuntime](https://github.com/microsoft/onnxruntime) (open-source): Used as an accelerator engine for the AI models.

<a href="https://github.com/google/sentencepiece">SentencePiece</a> (open-source): Used for tokenization of the input text for NLLB.

<a href="https://developers.google.com/ml-kit/language/identification">Ml Kit</a> (closed-source): Used for the identification of the language in the WalkieTalkie mode.
<br /><br />
And the following AI models:

<a href="https://github.com/facebookresearch/fairseq/tree/nllb">NLLB</a> (open-source, but only for non-commercial use): The model used is NLLB-Distilled-600M with KV cache.

<a href="https://github.com/openai/whisper">Whisper</a> (open-source): The model used is Whisper-Small-244M with KV cache.
<br /><br />

<h3>Performance of the models</h3>

I converted both NLLB and Whisper to onnx format and quantized them in int8 (excluding some weights to ensure almost zero quality loss), I also separated some parts of the models to reduce RAM consumption (without this separation some weights were duplicated at runtime consuming more RAM than expected) and done other optimizations to reduce execution time.

Here are the results of my optimizations:

|         | normal NLLB onnx model <br />(full int8, no kv-cache)  | RTranslator NLLB onnx model <br /> (partial int8, with kv-cache, separated parts)  |
|---------| ------------------------------------------- | ---------------------------------------------------------- |
|RAM Consumption| 2.5 GB  | 1.3GB &nbsp;&nbsp;(1.9x improvement)  |
|Execution time for 75 tokens| 8s  | 2s &nbsp;&nbsp;(4x improvement)  |

|         | Whisper onnx model optimized with [Olive](https://github.com/microsoft/Olive) <br /> (full int8, with kv-cache)  | RTranslator Whisper onnx model <br /> (partial int8, with kv-cache, separated parts)  |
|---------| -------------------------------------------------------------- | ------------------------------------------------------------- |
|RAM Consumption| 1.4 GB  | 0.9 GB &nbsp;&nbsp;(1.5x improvement)|
|Execution time for 11s audio| 1.9s  | 1.6s &nbsp;&nbsp;(1.2x improvement)|

**N.B.** RTranslator Whisper model can also consume 0.5 GB of RAM but with an execution time of 2.1s (this mode is used for phones with less than 8 GB of RAM)
<br /><br />

<h3>Donations</h3>

This is an open-source and completely ad-free app, I don't make any money from it.

So, if you like the app and want to say thank you and support the project, you can make a donation by clicking on one of the the buttons below (any amount is well accepted).

[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/niedev)

<a href='https://www.paypal.com/donate/?business=3VBKS3WC6AFHN&no_recurring=0&currency_code=EUR'><img alt='Donate' src='https://raw.githubusercontent.com/niedev/RTranslator/v2.00/images/Paypal.png' style="width: 190px; height: 80px;" /></a>

In case you will donate, or just live a star, thank you :heart:
<br /><br />

<h3>Bugs and problems</h3>
I remind you that the app is still in beta. The bugs found are the following:

- Sometimes the Bluetooth connection drops.

If you have found any bug please report it by opening an issue, or by writing an email to contact.niedev@gmail.com
<br /><br />

Enjoy your simultaneous translator.
