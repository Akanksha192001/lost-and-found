// Utility function to format dates consistently across the app
export const formatDate = (dateString) => {
  if (!dateString) return 'Not specified';
  
  try {
    // If it's already in YYYY-MM-DD format, convert to readable format
    const date = new Date(dateString);
    
    // Check if date is valid
    if (isNaN(date.getTime())) {
      return dateString; // Return original if can't parse
    }
    
    // Format as readable date
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric' 
    });
  } catch (error) {
    console.warn('Error formatting date:', dateString, error);
    return dateString; // Return original if error
  }
};

// Function to get today's date in YYYY-MM-DD format for form inputs
export const getTodayDate = () => {
  const today = new Date();
  return today.getFullYear() + '-' + 
         String(today.getMonth() + 1).padStart(2, '0') + '-' + 
         String(today.getDate()).padStart(2, '0');
};