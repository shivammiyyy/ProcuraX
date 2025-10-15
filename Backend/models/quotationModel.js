import mongoose from "mongoose";

const quotationSchema = new mongoose.Schema(
  {
    rfq: { type: mongoose.Schema.Types.ObjectId, ref: 'Rfq', required: true },
    vendor: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    price: { type: Number, required: true },
    deliveryTimeDays: {
        type: Number,
        required: true,
    },
    compliance: [
      {
        ISO_Certification: { type: Boolean, required: true },
        Material_Grade: { type: String, required: true , enum: ['A+', 'A', 'B', 'C']},
        Environmental_Standards: { type: Boolean, required: true },
        Document_Submission: { type: Boolean, required: true },
      },
    ],
    vendorScore: { type: Number, default: 0 },
    attachments: [
        {
            fileName: String,
            filePath: String,
        }
    ],
    status: { type: String, enum: ['submitted', 'under_review','Contract_created', 'accepted', 'rejected'], default: 'submitted' },
  },
  {
    timestamps: true,
  }
);

const Quotation = mongoose.model("Quotation", quotationSchema);
export default Quotation;
