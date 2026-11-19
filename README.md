# ExplainItAI

ExplainItAI is a PDF-based study assistant that consists of:

- An Android application built with Jetpack Compose and Kotlin
- A Node.js/Express backend that extracts PDF text and communicates with an Ollama AI model

Users can select a PDF, view and zoom through its pages, extract text from the selected page, chat with the page content, and generate multiple-choice questions.

## Features

- Upload PDF files from the device
- Keep a recent PDF history inside the app
- Render PDF pages with Android `PdfRenderer`
- Navigate between pages and zoom/pan the document
- Extract text for a specific page through the backend
- Ask questions about the current page
- Generate a custom number of AI-created MCQs
- Review quiz scores and answer correctness
- Use local Ollama inference without sending prompts to a cloud AI service

## Project Structure

```text
ExplainAI/
├── Backend/
│   ├── server.js
│   ├── package.json
│   └── package-lock.json
├── ExplainitAI/
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/dinesh/explainitai/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── README.md
```

## Technology Stack

### Android app

- Kotlin
- Jetpack Compose
- Material 3
- Retrofit 2
- OkHttp
- Gson
- Android PdfRenderer
- Markwon
- SharedPreferences-based PDF history

### Backend

- Node.js
- Express 5
- Multer
- pdf-parse
- Axios
- Ollama REST API

## Prerequisites

Install or prepare the following:

1. **Node.js** and npm
2. **Ollama**
3. **Android Studio** with an Android SDK
4. A physical Android device or emulator
5. The Android device and the computer running the backend must be able to reach each other

## Ollama Setup

Start Ollama and confirm that the required model is available:

```powershell
ollama list
ollama pull qwen2.5-coder:7b
ollama serve
```

The backend currently uses:

```text
Host:  http://localhost:11434
Model: qwen2.5-coder:7b
```

To use another model, change `MODEL_NAME` in `Backend/server.js`.

## Backend Setup

Open a terminal in the `Backend` directory:

```powershell
cd Backend
npm install
node server.js
```

The server listens on all network interfaces at:

```text
http://0.0.0.0:3000
```

## Android App Setup

1. Open the `ExplainitAI` folder in Android Studio.
2. Sync the Gradle project.
3. Build and run the app on a physical device or emulator.

### Configure the Backend IP Address

The Android app currently uses this hard-coded address:

```text
http://192.168.29.111:3000/
```

Update `BASE_URL` in:

```text
ExplainitAI/app/src/main/java/com/dinesh/explainitai/network/ApiService.kt
```

Examples:

- Physical device: `http://<computer-LAN-IP>:3000/`
- Android emulator: `http://10.0.2.2:3000/`

If a physical device is used, the Android and backend machines must be on the same Wi-Fi network.

Windows users can find their LAN IP with:

```powershell
ipconfig
```

If needed, update `ExplainitAI/app/src/main/res/xml/network_security_config.xml` so cleartext HTTP access is permitted for the backend address.

## Backend API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/extract-page` | Upload a PDF and extract text from a page number |
| `POST` | `/api/explain` | Request an AI explanation of supplied text |
| `POST` | `/api/generate-mcq` | Generate a requested number of MCQs |
| `POST` | `/api/chat-page` | Ask a question about supplied page text |

### Extract a PDF Page

Send a multipart request containing:

- `pdf`: PDF file
- `pageNumber`: one-based page number

### Explain Text

```json
{
  "text": "Text to explain"
}
```

### Generate MCQs

```json
{
  "text": "Text used to generate questions",
  "count": 5
}
```

### Chat With a Page

```json
{
  "pageText": "Extracted textbook page text",
  "question": "Explain this topic"
}
```

## Application Workflow

1. Upload or select a previously saved PDF.
2. Browse pages using the page controls.
3. Zoom or pan to inspect the page.
4. Open page chat or generate MCQs.
5. Chat receives the extracted text for the current page.
6. MCQs are generated using the user-selected question count.
7. Submit answers and review the final score and correct answers.

## Development Notes

- PDF uploads are kept in memory on the backend using Multer memory storage.
- Uploaded PDFs are not permanently stored by the backend.
- PDFs are copied into the Android app's internal storage for recent-file history.
- `SelectPage` sends the complete PDF to the backend for text extraction because page text is not extracted locally.
- The MCQ endpoint limits extracted text to the first 4,000 characters before sending it to Ollama.
- The app uses cleartext HTTP because the backend is intended for local-network development.
- Network timeouts are set to 180 seconds to allow for slower local AI generation.

## Troubleshooting

### Android app cannot connect to the backend

- Confirm `node server.js` is running.
- Confirm the backend port is `3000`.
- Replace the sample IP with the computer's current LAN IP.
- Make sure both devices are on the same network.
- Check Windows Firewall permissions for Node.js or port `3000`.
- Verify the app's network security configuration permits the backend host.

### Ollama request fails

- Confirm Ollama is running.
- Run `ollama list` and verify the configured model exists.
- Try `ollama pull qwen2.5-coder:7b` if the model is missing.
- Confirm port `11434` is reachable locally from the backend process.

### PDF text extraction fails

- Try another PDF or a smaller PDF.
- Confirm the PDF contains selectable text; scanned image-only PDFs may need OCR.
- Check the Android upload and backend server logs.

## License

No license has been specified for this project.






# Important Note

Before running this code please change the IP address in ApiService.kt and in network_security_config.xml

1. Open ollama
2. Go to backend folder and open terminal and run "node .\server.js"
3. Check ip address of Wifi using ipconfig and use ipv4 address 
4. Change the ip address in networks/ApiService.kt and res/xml/network_security_config.xml 
