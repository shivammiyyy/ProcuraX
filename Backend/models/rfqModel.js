import mongoose from "mongoose";

//for both rfq and rfp
const rfqSchema = new mongoose.Schema(
  {
    title: { type: String, required: true },
    description: { type: String, required: true },
    requestType: { type: String, required: true, enum: ['RFQ', 'RFP'] },
    budget: { type: Number, required: true },
    deadline: { type: Date, required: true },
    category: { type: String, required: true , enum: ['Office Supplies', 'IT Hardware', 'Raw Materials']},
    status: { type: String, enum: ['open', 'in_progress', 'closed'], default: 'open' },
    Buyer: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    attachment: [ 
    {
      fileName: String,
      filePath: String,
    }
  ],

  },
  {
    timestamps: true,
  }
);

const Rfq = mongoose.model("Rfq", rfqSchema);
export default Rfq;
