import { GoogleGenerativeAI } from '@google/generative-ai';
import dotenv from 'dotenv';

dotenv.config();

const API_KEY = process.env.GEMINI_API_KEY;

if (!API_KEY) {
  console.warn('⚠️ GEMINI_API_KEY is not set. Contract audit functionality will be limited.');
}

const genAI = new GoogleGenerativeAI(API_KEY);
const model = genAI.getGenerativeModel({ model: 'gemini-pro' });

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
    Provide:
    1. An overall summary report of your findings.
    2. A list of specific warnings, each with a 'type' (e.g., 'MissingClause', 'VagueTerm', 'FinancialInconsistency', 'ExpiredTerm', 'ComplianceIssue'), a 'description' of the issue, and a 'severity' (Low, Medium, High).
    3. An overall audit status (e.g., 'Completed', 'NeedsReview', 'CriticalIssues').

    Contract Content:
    ---
    ${contractContent}
    ---

    Respond in a JSON format like this:
    {
      "auditReport": "...",
      "auditWarnings": [
        {"type": "...", "description": "...", "severity": "..."},
        {"type": "...", "description": "...", "severity": "..."}
      ],
      "auditStatus": "..."
    }
  `;

  try {
    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    let jsonString = text.replace(/``````/g, '').trim();
    const auditResult = JSON.parse(jsonString);

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
    return {
      auditReport: `An error occurred during AI audit: ${error.message}`,
      auditWarnings: [
        {
          type: 'LLMFailure',
          description: `Failed to process contract with AI. Error: ${error.message}`,
          severity: 'High',
        },
      ],
      auditStatus: 'Failed',
    };
  }
};
