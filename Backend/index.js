import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import connectDB from './config/mongodb.js';
import authRoutes from './routes/authRoutes.js';
import rfqRoutes from './routes/rfqRoutes.js';
import quotationRoutes from './routes/quotationRoutes.js';
import contractRoutes from './routes/contractRoutes.js';

dotenv.config();

await connectDB(); // ensure DB connects before starting (connectDB returns a Promise)

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json()); // built-in body parser

app.get('/', (req, res) => res.send('Hello World!'));
app.use('/api/v0/auth', authRoutes);
app.use('/api/v0/rfq', rfqRoutes);
app.use('/api/v0/quotation', quotationRoutes);
app.use('/api/v0/contract', contractRoutes);

// global error handler (minimal)
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({ message: 'Internal Server Error' });
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
