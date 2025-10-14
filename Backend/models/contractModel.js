import mongoose from "mongoose";

const contractSchema = new mongoose.Schema(
  {
    rfq: { type: mongoose.Schema.Types.ObjectId, ref: 'Rfq', required: true },
    vendor: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    buyer: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    quotation: { type: mongoose.Schema.Types.ObjectId, ref: 'Quotation', required: true },
    content: { type: String, required: true }, // Full text content of the contract
    contractFile: { // Sub-document for uploaded contract
        type: {
            fileName: { type: String, required: true },
            filePath: { type: String, required: true },
        },
    },
    startDate: { type: Date, required: true },
    endDate: { type: Date, required: true },
    status: { // 'Active', 'Expired', 'Audited', 'Cancelled'
        type: String,
        default: 'Active',
        enum: ['Active', 'Expired', 'Audited', 'Cancelled'],
    },
    // Fields for LLM-based Contract Audit
    auditStatus: { // 'Pending', 'InProgress', 'Completed', 'Failed'
        type: String,
        default: 'Pending',
        enum: ['Pending', 'InProgress', 'Completed', 'Failed'],
    },
    auditReport: { // Store the insights from the LLM
        type: String, // Can be JSON string or just text
    },
    auditWarnings: [ // Array of issues found by the LLM
        {
            type: {
                warningType: { type: String }, // e.g., 'MissingClause', 'MismatchValue', 'ExpiredTerm'
                description: { type: String },
                severity: { type: String, enum: ['Low', 'Medium', 'High'] },
            }
        }
    ],
  },
  {
    timestamps: true,
  }
);

const Contract = mongoose.model("Contract", contractSchema);
export default Contract;
