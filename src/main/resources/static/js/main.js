// main.js - JavaScript cho OpenEvent UI
document.addEventListener('DOMContentLoaded', function() {
    
    // Carousel functionality
    const carousels = document.querySelectorAll('.carousel');
    
    carousels.forEach(carousel => {
        const track = carousel.querySelector('.carousel-track');
        const prevBtn = carousel.querySelector('.carousel-nav button:first-child');
        const nextBtn = carousel.querySelector('.carousel-nav button:last-child');
        const pageSpan = carousel.querySelector('.carousel-nav span');
        
        if (!track || !prevBtn || !nextBtn) return;
        
        let currentIndex = 0;
        const cards = track.querySelectorAll('.card');
        const totalCards = cards.length;
        const cardsPerPage = 4; // Số card hiển thị mỗi lần
        const totalPages = Math.ceil(totalCards / cardsPerPage);
        
        function updateCarousel() {
            const translateX = -currentIndex * (100 / cardsPerPage);
            track.style.transform = `translateX(${translateX}%)`;
            
            if (pageSpan) {
                pageSpan.textContent = `${currentIndex + 1} / ${totalPages}`;
            }
            
            // Update focus state
            cards.forEach((card, index) => {
                card.classList.toggle('is-focus', 
                    index >= currentIndex && index < currentIndex + cardsPerPage);
            });
        }
        
        prevBtn.addEventListener('click', () => {
            if (currentIndex > 0) {
                currentIndex--;
                updateCarousel();
            }
        });
        
        nextBtn.addEventListener('click', () => {
            if (currentIndex < totalPages - 1) {
                currentIndex++;
                updateCarousel();
            }
        });
        
        // Initialize
        updateCarousel();
    });
    
    // Filter chips functionality
    const chipGroups = document.querySelectorAll('.chip-group');
    
    chipGroups.forEach(group => {
        const chips = group.querySelectorAll('.chip');
        
        chips.forEach(chip => {
            chip.addEventListener('click', () => {
                // Remove active class from all chips in this group
                chips.forEach(c => c.classList.remove('is-active'));
                // Add active class to clicked chip
                chip.classList.add('is-active');
                
                // Here you would typically filter content
                console.log('Filter changed to:', chip.textContent);
            });
        });
    });
    
    // Search functionality
    const searchInputs = document.querySelectorAll('.search input');
    
    searchInputs.forEach(input => {
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                const query = e.target.value.trim();
                if (query) {
                    console.log('Searching for:', query);
                    // Here you would implement actual search
                }
            }
        });
    });
    
    // Pager functionality
    const pagers = document.querySelectorAll('.pager');
    
    pagers.forEach(pager => {
        const prevBtn = pager.querySelector('button:first-child');
        const nextBtn = pager.querySelector('button:last-child');
        const pageSpan = pager.querySelector('span');
        
        if (!prevBtn || !nextBtn || !pageSpan) return;
        
        let currentPage = 1;
        const totalPages = 5; // This would come from backend
        
        function updatePager() {
            pageSpan.textContent = `${currentPage} / ${totalPages}`;
            prevBtn.disabled = currentPage === 1;
            nextBtn.disabled = currentPage === totalPages;
        }
        
        prevBtn.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                updatePager();
                console.log('Go to page:', currentPage);
            }
        });
        
        nextBtn.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                updatePager();
                console.log('Go to page:', currentPage);
            }
        });
        
        updatePager();
    });
    
    // Smooth scrolling for anchor links
    const anchorLinks = document.querySelectorAll('a[href^="#"]');
    
    anchorLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = link.getAttribute('href').substring(1);
            const targetElement = document.getElementById(targetId);
            
            if (targetElement) {
                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Add loading states to buttons
    const buttons = document.querySelectorAll('.btn');
    
    buttons.forEach(button => {
        button.addEventListener('click', function() {
            if (this.href === '#' || !this.href.includes('http')) {
                // Add loading state for internal buttons
                const originalText = this.textContent;
                this.textContent = 'Loading...';
                this.disabled = true;
                
                // Simulate loading
                setTimeout(() => {
                    this.textContent = originalText;
                    this.disabled = false;
                }, 1000);
            }
        });
    });
    
    // Add hover effects to cards
    const cards = document.querySelectorAll('.card');
    
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px)';
            this.style.transition = 'transform 0.2s ease';
        });
        
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
        });
    });
    
    console.log('OpenEvent UI initialized successfully!');
});
