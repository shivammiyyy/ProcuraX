import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import connectDB from './config/mongodb.js';
import authRoutes from './routes/authRoutes.js';
import rfqRoutes from './routes/rfqRoutes.js';
import quotationRoutes from './routes/quotationRoutes.js';
import contractRoutes from './routes/contractRoutes.js';

dotenv.config();

const startServer = async () => {
  try {
    await connectDB(); // Connect to MongoDB before starting server

    const app = express();
    const PORT = process.env.PORT || 3000;

    // Configure CORS - restrict origins in production as needed
    app.use(
      cors({
        origin: process.env.FRONTEND_URL || '*', // Replace '*' with frontend URL in production
        credentials: true,
      })
    );

    app.use(express.json()); // Body parser for JSON

    // Add routes
    app.get('/', (req, res) => res.send('Hello World!'));
    app.use('/api/v0/auth', authRoutes);
    app.use('/api/v0/rfq', rfqRoutes);
    app.use('/api/v0/quotation', quotationRoutes);
    app.use('/api/v0/contract', contractRoutes);

    // Minimal centralized error handler middleware
    app.use((err, req, res, next) => {
      console.error('Unhandled error:', err);
      res.status(500).json({ message: 'Internal Server Error' });
    });

    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
};

startServer();
