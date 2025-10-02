const tickets = document.querySelectorAll(".ticket");
const orderList = document.getElementById("order-list");
const totalDisplay = document.getElementById("total");

tickets.forEach(ticket => {
    const plusBtn = ticket.querySelector(".plus");
    const minusBtn = ticket.querySelector(".minus");
    const countSpan = ticket.querySelector(".count");

    plusBtn.addEventListener("click", () => {
        tickets.forEach(t => {
            t.querySelector(".count").textContent = "0";
            t.classList.remove("selected");
        });
        countSpan.textContent = "1";
        ticket.classList.add("selected");
        updateSummary();
    });

    minusBtn.addEventListener("click", () => {
        countSpan.textContent = "0";
        ticket.classList.remove("selected");
        updateSummary();
    });
});
const ticketFilter = document.getElementById("ticketFilter");

ticketFilter.addEventListener("change", () => {
    const value = ticketFilter.value;
    tickets.forEach(ticket => {
        if (value === "all" || ticket.dataset.category === value) {
            ticket.style.display = "block";
        } else {
            ticket.style.display = "none";
        }
    });
});


function updateSummary() {
    let selectedTicket = null;

    tickets.forEach(ticket => {
        const count = parseInt(ticket.querySelector(".count").textContent);
        if (count > 0) {
            selectedTicket = {
                type: ticket.dataset.type,
                price: parseFloat(ticket.dataset.price),
                count: count
            };
        }
    });

    orderList.innerHTML = "";

    if (selectedTicket) {
        const subtotal = selectedTicket.price * selectedTicket.count;
        const vat = subtotal * 0.10;
        const total = subtotal + vat;

        const li1 = document.createElement("li");
        li1.innerHTML = `<span>${selectedTicket.count} x ${capitalize(selectedTicket.type)} Entry</span><span>$${subtotal.toFixed(2)}</span>`;

        const li2 = document.createElement("li");
        li2.innerHTML = `<span>Subtotal</span><span>$${subtotal.toFixed(2)}</span>`;

        const li3 = document.createElement("li");
        li3.innerHTML = `<span>VAT (10%)</span><span>$${vat.toFixed(2)}</span>`;

        orderList.appendChild(li1);
        orderList.appendChild(li2);
        orderList.appendChild(li3);

        totalDisplay.textContent = `Total $${total.toFixed(2)}`;
    } else {
        const li = document.createElement("li");
        li.innerHTML = `<span>No tickets selected</span><span>$0.00</span>`;
        orderList.appendChild(li);
        totalDisplay.textContent = "Total $0.00";
    }
}

function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

document.getElementById("checkoutBtn").addEventListener("click", async () => {
    const selectedMethod = document.querySelector('input[name="payment"]:checked')?.value;
    const totalText = document.getElementById("total").textContent;
    const amount = parseFloat(totalText.replace("Total $", "")) * 1000;

    if (amount <= 0) {
        alert("Please select a ticket first!");
        return;
    }

    if (!selectedMethod) {
        alert("Please select a payment method!");
        return;
    }

    if (selectedMethod === "payos") {
        try {
            const res = await fetch("/api/create-payment", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ amount })
            });
            const data = await res.json();
            if (data.checkoutUrl) {
                window.location.href = data.checkoutUrl;
            } else {
                alert("Failed to create PayOS payment");
            }
        } catch (err) {
            console.error(err);
            alert("Error creating PayOS payment");
        }
    } else {
        alert("Payment method: " + selectedMethod + " (not implemented yet)");
    }
});

document.querySelectorAll(".see-more-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        const ticket = btn.closest(".ticket");
        ticket.classList.toggle("expanded");
        btn.textContent = ticket.classList.contains("expanded") ? "See less" : "See more";
    });
});