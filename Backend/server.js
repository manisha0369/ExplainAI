const express = require('express');
const multer = require('multer');
const pdfParse = require('pdf-parse');
const axios = require('axios');

const app = express();
app.use(express.json());

// Configure multer for handling PDF file uploads in memory
const upload = multer({ storage: multer.memoryStorage() });

// Ollama configuration
const OLLAMA_HOST = 'http://localhost:11434';
const MODEL_NAME = 'qwen2.5-coder:7b'; // Change this to match your `ollama list` model name

// Helper function to query Ollama
async function queryOllama(prompt) {
  try {
    const response = await axios.post(`${OLLAMA_HOST}/api/generate`, {
      model: MODEL_NAME,
      prompt: prompt,
      stream: false
    });
    return response.data.response;
  } catch (error) {
    console.error('Ollama Error:', error.message);
    throw new Error('Failed to communicate with AI model.');
  }
}

// 1. Extract text from a specific PDF page
app.post('/api/extract-page', upload.single('pdf'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No PDF file uploaded.' });
    }
    const pageNumber = parseInt(req.body.pageNumber) || 1;

    // Parse PDF text page-by-page
    const pdfData = await pdfParse(req.file.buffer);
    const textPages = pdfData.text.split(/\f/); // Form feed splits pages in pdf-parse

    const targetPageIndex = pageNumber - 1;
    const pageText = textPages[targetPageIndex] || pdfData.text; // Fallback to full text if page not found

    res.json({
      totalPages: textPages.length,
      pageNumber: pageNumber,
      pageText: pageText.trim()
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// 2. Explain page text (AI Teacher Breakdown) - Clean text formatting
app.post('/api/explain', async (req, res) => {
  try {
    const { text } = req.body;
    if (!text) return res.status(400).json({ error: 'Text is required.' });

    const prompt = `You are an expert, friendly AI study tutor. Break down and explain the following study text clearly with bullet points and headers. 
CRITICAL RULE: Do NOT use LaTeX syntax or math blocks (like \\text{}, \\rightarrow, \\mu, or \\cdot). Use normal plain text characters, standard arrows (->), and normal dots (.) for chemical or mathematical equations so they look clean and readable.

Study Text:
${text}

Explanation:`;

    const explanation = await queryOllama(prompt);
    res.json({ explanation });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});




// 3. Generate Multiple Choice Questions (MCQs) with dynamic count and robust error handling
app.post('/api/generate-mcq', async (req, res) => {
  try {
    let { text, count } = req.body;
    if (!text) return res.status(400).json({ error: 'Text is required.' });

    // Safely parse the requested question count from the dialog popup
    const questionCount = parseInt(count) || 3;
    console.log(`Generating exactly ${questionCount} MCQs for text length: ${text.length}`);

    // SAFETY CAP: Truncate if text is excessively large for local LLM context window
    if (text.length > 4000) {
      console.warn('Text is too long. Truncating to first 4000 characters.');
      text = text.substring(0, 4000);
    }

    const prompt = `Based on the following text, generate a JSON array of EXACTLY ${questionCount} multiple-choice questions. 
CRITICAL RULES:
1. Return ONLY valid JSON. No conversational intro, no extra text outside the JSON.
2. Structure must match this exact format with exactly ${questionCount} items inside the "mcqs" array:
{
  "mcqs": [
    {
      "question": "Question text here?",
      "options": [
        "A) Option 1",
        "B) Option 2",
        "C) Option 3",
        "D) Option 4"
      ],
      "correctAnswer": "A"
    }
  ]
}

Text:
${text}`;

    const rawResponse = await queryOllama(prompt);
    console.log('Raw Ollama MCQ Response received.');

    let cleaned = rawResponse.replace(/```json/gi, '').replace(/```/g, '').trim();
    
    const firstBrace = cleaned.indexOf('{');
    if (firstBrace === -1) {
      throw new Error('Model did not return a JSON object.');
    }

    let parsedData = null;
    for (let i = cleaned.length; i > firstBrace; i--) {
      if (cleaned[i] === '}') {
        const candidate = cleaned.substring(firstBrace, i + 1);
        try {
          parsedData = JSON.parse(candidate);
          break;
        } catch (e) {
          // Keep trying earlier closing braces if necessary
        }
      }
    }

    if (!parsedData) {
      let jsonString = cleaned.substring(firstBrace, cleaned.lastIndexOf('}') + 1);
      parsedData = JSON.parse(jsonString);
    }

    console.log(`Successfully parsed ${parsedData.mcqs?.length || 0} MCQs.`);
    res.json(parsedData);
  } catch (error) {
    console.error('MCQ Generation Server Error:', error.message);
    res.status(500).json({ error: error.message });
  }
});

// 4. Chat with Book Page Endpoint - Clean plain text chemical formulas & short answers
app.post('/api/chat-page', async (req, res) => {
  try {
    const { pageText, question } = req.body;
    if (!pageText || !question) {
      return res.status(400).json({ error: 'Page text and question are required.' });
    }

    const prompt = `You are an expert, helpful AI study assistant. Below is the reference text from a textbook page the student is reading. Answer the student's question clearly and concisely based on this text.
CRITICAL RULES: 
1. Keep your answers clear, structured, and short unless asked for a detailed explanation.
2. Do NOT use LaTeX formatting or math tags (like \\text{}, \\rightarrow, or \\cdot). Use normal plain text characters, standard arrows (->), and normal dots (.) for chemical or scientific equations.

Reference Text:
${pageText}

Student's Question: ${question}

Answer:`;

    const answer = await queryOllama(prompt);
    res.json({ answer });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on port ${PORT}`);
});