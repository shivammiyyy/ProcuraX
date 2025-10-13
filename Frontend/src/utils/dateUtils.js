// Date and time related helpers

export const formatDate = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleDateString();
};

export const formatDateTime = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleString();
};

export const daysRemaining = (dateString) => {
  const now = new Date();
  const targetDate = new Date(dateString);
  const diffTime = targetDate.getTime() - now.getTime();
  return Math.ceil(diffTime / (1000 * 3600 * 24));
};
