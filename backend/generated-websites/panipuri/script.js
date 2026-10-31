// script.js – Panipuri stall interactive features
// ---------------------------------------------------
// This script handles:
//   • Smooth scrolling for navigation links
//   • Opening/closing the order modal
//   • Adding items to a simple cart, displaying it and calculating total
//   • Checkout (demo alert)
// All interactions are vanilla JS – no external dependencies.

document.addEventListener('DOMContentLoaded', () => {
  // ---------- Smooth scrolling for navigation ----------
  document.querySelectorAll('.nav a[href^="#"]').forEach(link => {
    link.addEventListener('click', e => {
      e.preventDefault();
      const target = document.querySelector(link.getAttribute('href'));
      if (target) {
        target.scrollIntoView({ behavior: 'smooth' });
      }
    });
  });

  // ---------- Modal handling ----------
  const orderModal = document.getElementById('orderModal');
  const openButtons = [document.getElementById('orderBtn'), document.getElementById('heroOrderBtn')];
  const closeModal = document.getElementById('closeModal');

  const openModal = () => orderModal.classList.add('show');
  const hideModal = () => orderModal.classList.remove('show');

  openButtons.forEach(btn => btn && btn.addEventListener('click', openModal));
  closeModal && closeModal.addEventListener('click', hideModal);
  // Close modal when clicking outside the content
  orderModal && orderModal.addEventListener('click', e => {
    if (e.target === orderModal) hideModal();
  });

  // ---------- Cart logic ----------
  const cart = [];
  const cartContainer = document.getElementById('cartItems');
  const totalEl = document.getElementById('totalAmount');

  const updateCartUI = () => {
    // Clear current list
    cartContainer.innerHTML = '';
    let total = 0;
    cart.forEach((item, idx) => {
      const itemDiv = document.createElement('div');
      itemDiv.className = 'cart-item';
      itemDiv.innerHTML = `
        <span>${item.name} x ${item.qty}</span>
        <span>₹${item.price * item.qty}</span>
        <button class="remove-item" data-index="${idx}">&times;</button>
      `;
      cartContainer.appendChild(itemDiv);
      total += item.price * item.qty;
    });
    totalEl.textContent = `₹${total}`;
    // Attach remove listeners
    cartContainer.querySelectorAll('.remove-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const i = parseInt(btn.dataset.index, 10);
        cart.splice(i, 1);
        updateCartUI();
      });
    });
  };

  // Add to cart from menu items
  document.querySelectorAll('.menu-item .add-to-cart').forEach(button => {
    button.addEventListener('click', () => {
      const itemEl = button.closest('.menu-item');
      const name = itemEl.dataset.name;
      const price = parseInt(itemEl.dataset.price, 10);
      const existing = cart.find(i => i.name === name);
      if (existing) {
        existing.qty += 1;
      } else {
        cart.push({ name, price, qty: 1 });
      }
      updateCartUI();
    });
  });

  // ---------- Checkout ----------
  const checkoutBtn = document.getElementById('checkoutBtn');
  checkoutBtn && checkoutBtn.addEventListener('click', () => {
    if (cart.length === 0) {
      alert('Your cart is empty! Add some delicious panipuri first.');
      return;
    }
    const summary = cart.map(i => `${i.name} x${i.qty} – ₹${i.price * i.qty}`).join('\n');
    const total = cart.reduce((sum, i) => sum + i.price * i.qty, 0);
    alert(`Thank you for ordering!\n\nYour order:\n${summary}\n\nTotal: ₹${total}\n\nWe will contact you shortly.`);
    // Reset cart
    cart.length = 0;
    updateCartUI();
    hideModal();
  });
});
