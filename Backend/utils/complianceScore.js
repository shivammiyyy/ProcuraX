import * as ort from 'onnxruntime-node'; // ✅ use node version, not web

import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const MODEL_PATH = path.join(__dirname, '../ml_model/vendor_model.onnx'); // ✅ Correct local path

let session = null;

export const loadModel = async () => {
  if (session) return session;
  try {
    session = await ort.InferenceSession.create(MODEL_PATH);
    console.log('✅ ONNX model loaded successfully');
    return session;
  } catch (error) {
    console.error('❌ Failed to load ONNX model:', error);
    throw error;
  }
};

export const predictVendorScore = async (inputArray) => {
  try {
    const session = await loadModel();

    const inputTensor = new ort.Tensor('float32', new Float32Array(inputArray), [1, 3]);
    const feeds = { input: inputTensor };

    const results = await session.run(feeds);
    const output = results[Object.keys(results)[0]].data[0];

    return parseFloat(output.toFixed(2));
  } catch (error) {
    console.error('❌ Error during ONNX inference:', error);
    return null;
  }
};
