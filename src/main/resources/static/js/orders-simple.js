// Simple orders.js test
console.log('🔥 SIMPLE orders.js loaded!');
alert('SIMPLE orders.js is loading!');

// Simple functions
window.selectStatusFilter = function(element) {
    console.log('🔍 Simple selectStatusFilter called');
    alert('Status filter clicked: ' + element.getAttribute('data-status'));
};

window.viewOrderDetail = function(button) {
    console.log('🔍 Simple viewOrderDetail called');
    const orderId = button.getAttribute('data-order-id');
    alert('View order detail: ' + orderId);
};

console.log('✅ Simple functions assigned');
