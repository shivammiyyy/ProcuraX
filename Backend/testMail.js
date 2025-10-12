import { predictComplianceScore } from './utils/complianceScore.js';

(async () => {
  const input = [1000, 8, 92]; // Example input: PriceDiff, DeliveryDays, ComplianceScore
  const result = await predictComplianceScore(input);
  console.log("🎯 Predicted Vendor Final Score:", result);
})();