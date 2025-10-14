import { GoogleGenAI } from '@google/genai';
import dotenv from 'dotenv';

dotenv.config();

const API_KEY = process.env.GEMINI_API_KEY;

if (!API_KEY) {
  console.warn('⚠️ GEMINI_API_KEY is not set. Contract audit functionality will be limited.');
}

// Initialize the client with API key
const ai = new GoogleGenAI({ apiKey: API_KEY });

/**
 * Conducts an LLM-based audit of a contract document content.
 * @param {string} contractContent - The text content of the contract to audit.
 * @returns {Promise<object>} An object containing audit report, warnings, and overall status.
 */
export const auditContractWithGemini = async (contractContent) => {
  if (!API_KEY) {
    return {
      auditReport: 'API key not configured. Cannot perform audit.',
      auditWarnings: [
        {
          type: 'ConfigurationError',
          description: 'GEMINI_API_KEY is missing in environment variables.',
          severity: 'High',
        },
      ],
      auditStatus: 'Failed',
    };
  }

  const prompt = `
    You are an AI contract auditor. Review the following contract content and identify any potential issues, inconsistencies, missing clauses, or areas of concern.
    
    IMPORTANT: Respond ONLY with valid JSON. Do not include any markdown formatting, code blocks, or explanatory text.
    
    Provide:
    1. An overall summary report of your findings (keep it concise, under 200 words).
    2. A list of specific warnings, each with a 'type' (e.g., 'MissingClause', 'VagueTerm', 'FinancialInconsistency', 'ExpiredTerm', 'ComplianceIssue'), a 'description' of the issue, and a 'severity' (Low, Medium, High).
    3. An overall audit status (must be one of: 'Completed', 'NeedsReview', 'CriticalIssues').

    Contract Content:
    ---
    ${contractContent}
    ---

    Respond with ONLY this JSON format (no other text):
    {
      "auditReport": "single line summary here",
      "auditWarnings": [
        {"type": "type here", "description": "description here", "severity": "Low/Medium/High"}
      ],
      "auditStatus": "Completed/NeedsReview/CriticalIssues"
    }
  `;

  try {
    // Use the new SDK format
    const response = await ai.models.generateContent({
      model: 'gemini-2.0-flash-exp',
      contents: prompt,
      config: {
        temperature: 0,
        maxOutputTokens: 1000,
        responseMimeType: 'application/json', // Request JSON response
      }
    });
    
    // Extract the text from the response
    let text = response.text;
    
    // Log raw response for debugging
    // console.log('Raw Gemini Response:', text);
    
    // Clean the response text
    // Remove markdown code blocks
    text = text.replace(/```json\n?/g, '').replace(/```\n?/g, '');
    
    // Find JSON object in the response (in case there's extra text)
    const jsonMatch = text.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      throw new Error('No valid JSON found in response');
    }
    
    const jsonString = jsonMatch[0].trim();
    
    // Parse the JSON
    const auditResult = JSON.parse(jsonString);

    // Validate structure
    if (!auditResult.auditReport || !Array.isArray(auditResult.auditWarnings) || !auditResult.auditStatus) {
      throw new Error('Gemini response did not match expected JSON structure.');
    }

    return {
      auditReport: auditResult.auditReport,
      auditWarnings: auditResult.auditWarnings,
      auditStatus: auditResult.auditStatus,
    };
  } catch (error) {
    console.error('Error auditing contract with Gemini:', error);
    
    // If JSON parsing failed, provide more details
    let errorMessage = error.message;
    if (error instanceof SyntaxError) {
      errorMessage = `JSON parsing failed: ${error.message}. Check the console for the raw response.`;
    }
    
    return {
      auditReport: `An error occurred during AI audit: ${errorMessage}`,
      auditWarnings: [
        {
          type: 'LLMFailure',
          description: `Failed to process contract with AI. Error: ${errorMessage}`,
          severity: 'High',
        },
      ],
      auditStatus: 'Failed',
    };
  }
};
