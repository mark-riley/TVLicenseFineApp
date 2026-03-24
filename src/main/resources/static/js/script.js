// sample dynamic values
const amountPaid = 121.00;
const amountRemaining = 0.00;
const transactionId = '#882-1942-001';
const paymentStatus = 'Completed';

document.getElementById('amount-paid').textContent = `£${amountPaid.toFixed(2)}`;
document.getElementById('amount-remaining').textContent = `£${amountRemaining.toFixed(2)}`;

document.getElementById('transaction-id').textContent = transactionId;
document.getElementById('payment-status').textContent = paymentStatus