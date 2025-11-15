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
            }
        });

        nextBtn.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                updatePager();
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

    // Add loading states to buttons (only for anchor tags with href)
    const buttons = document.querySelectorAll('a.btn');

    buttons.forEach(button => {
        button.addEventListener('click', function() {
            // Only process if it's an anchor tag with href
            if (this.href && (this.href === '#' || !this.href.includes('http'))) {
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

    // Use native date picker/icon; no JS injection here

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

});

// Hero Slider functionality
(function(){
    const track = document.getElementById('heroTrack');
    if(!track) return;

    const prev = document.getElementById('heroPrev');
    const next = document.getElementById('heroNext');
    const progressBars = document.querySelectorAll('.hero-progress-bar');
    const slideBy = () => window.innerWidth; // mỗi lần lướt 1 slide

    // Update progress indicator
    function updateProgress() {
        const scrollLeft = track.scrollLeft;
        const slideWidth = slideBy();
        const currentSlide = Math.round(scrollLeft / slideWidth);

        progressBars.forEach((bar, index) => {
            bar.classList.toggle('active', index === currentSlide);
        });
    }

    prev?.addEventListener('click', () =>
        track.scrollBy({ left: -slideBy(), behavior: 'smooth' })
    );

    next?.addEventListener('click', () =>
        track.scrollBy({ left: slideBy(), behavior: 'smooth' })
    );

    // Update progress on scroll
    track.addEventListener('scroll', updateProgress);

    // Initialize progress
    updateProgress();

    // Touch/swipe support for mobile
    let startX = 0;
    let isScrolling = false;

    track.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
        isScrolling = false;
    });

    track.addEventListener('touchmove', (e) => {
        if (!isScrolling) {
            const currentX = e.touches[0].clientX;
            const diffX = startX - currentX;

            if (Math.abs(diffX) > 10) {
                isScrolling = true;
            }
        }
    });

    track.addEventListener('touchend', (e) => {
        if (isScrolling) {
            const endX = e.changedTouches[0].clientX;
            const diffX = startX - endX;

            if (Math.abs(diffX) > 50) { // Minimum swipe distance
                if (diffX > 0) {
                    // Swipe left - go to next slide
                    track.scrollBy({ left: slideBy(), behavior: 'smooth' });
                } else {
                    // Swipe right - go to previous slide
                    track.scrollBy({ left: -slideBy(), behavior: 'smooth' });
                }
            }
        }
    });
})();

// Scroll detection for header blur effect
(function(){
    const onScroll = () => document.body.classList.toggle('has-scrolled', window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll);
})();

// Live & Recommendations interactions
(function(){
    // LIVE filter by tag (new horizontal scroll container)
    const liveContainerFilter = document.querySelector('.live-events-container');
    if(liveContainerFilter){
        document.querySelectorAll('.live-events-header .chip[data-filter]').forEach(ch=>{
            ch.addEventListener('click', ()=>{
                document.querySelectorAll('.live-events-header .chip[data-filter]').forEach(x=>x.classList.remove('is-active'));
                ch.classList.add('is-active');
                const filter = ch.dataset.filter;
                liveContainerFilter.querySelectorAll('.event-card-simple').forEach(card=>{
                    const tags = (card.dataset.tags || '').toLowerCase();
                    const ok = filter==='all' || tags.includes(filter.toLowerCase());
                    card.style.display = ok ? '' : 'none';
                });
                // Reset scroll position and trigger pagination update after filter
                setTimeout(() => {
                    if (liveContainerFilter) {
                        liveContainerFilter.scrollLeft = 0;
                        const scrollEvent = new Event('scroll');
                        liveContainerFilter.dispatchEvent(scrollEvent);
                        // Also trigger update manually after a short delay
                        setTimeout(() => {
                            liveContainerFilter.dispatchEvent(scrollEvent);
                        }, 100);
                    }
                }, 150);
            });
        });
    }
    
    // Legacy support for old liveTrack (if exists)
    const liveTrack = document.getElementById('liveTrack');
    const livePrev  = document.getElementById('livePrev');
    const liveNext  = document.getElementById('liveNext');
    if(liveTrack){
        const step = () => (liveTrack.querySelector('.live-card')?.getBoundingClientRect().width || 320) + 16;
        livePrev?.addEventListener('click', ()=> liveTrack.scrollBy({left: -step()*2, behavior:'smooth'}));
        liveNext?.addEventListener('click', ()=> liveTrack.scrollBy({left:  step()*2, behavior:'smooth'}));
        document.querySelectorAll('.chip[data-tag]').forEach(ch=>{
            ch.addEventListener('click', ()=>{
                document.querySelectorAll('.chip[data-tag]').forEach(x=>x.classList.remove('is-active'));
                ch.classList.add('is-active');
                const tag = ch.dataset.tag;
                liveTrack.querySelectorAll('.live-card').forEach(card=>{
                    const ok = tag==='all' || (card.dataset.tag||'').includes(tag);
                    card.style.display = ok ? '' : 'none';
                });
            });
        });
    }

    // RECO carousel + tabs
    const recoTrack = document.getElementById('recoTrack');
    const recoPrev  = document.getElementById('recoPrev');
    const recoNext  = document.getElementById('recoNext');
    if(recoTrack){
        const step = () => (recoTrack.querySelector('.reco-card')?.getBoundingClientRect().width || 240) + 14;
        recoPrev?.addEventListener('click', ()=> recoTrack.scrollBy({left:-step()*2, behavior:'smooth'}));
        recoNext?.addEventListener('click', ()=> recoTrack.scrollBy({left: step()*2, behavior:'smooth'}));
        document.querySelectorAll('.chip[data-reco]').forEach(ch=>{
            ch.addEventListener('click', ()=>{
                document.querySelectorAll('.chip[data-reco]').forEach(x=>x.classList.remove('is-active'));
                ch.classList.add('is-active');
                const tab = ch.dataset.reco;
                recoTrack.querySelectorAll('.reco-card').forEach(card=>{
                    card.style.display = (tab==='all' || (card.dataset.reason===tab)) ? '' : 'none';
                });
            });
        });
    }
})();

// Latest Events Horizontal Scroll
(function(){
    const latestContainer = document.querySelector('.latest-events-container');
    const prevBtn = document.getElementById('prevLatestBtn');
    const nextBtn = document.getElementById('nextLatestBtn');
    const paginationInfo = document.getElementById('latestPaginationInfo');
    
    if (!latestContainer || !prevBtn || !nextBtn || !paginationInfo) return;
    
    // LATEST filter by tag (similar to Live Events)
    document.querySelectorAll('.latest-events-header .chip[data-filter]').forEach(ch=>{
        ch.addEventListener('click', ()=>{
            document.querySelectorAll('.latest-events-header .chip[data-filter]').forEach(x=>x.classList.remove('is-active'));
            ch.classList.add('is-active');
            const filter = ch.dataset.filter;
            latestContainer.querySelectorAll('.event-card-simple').forEach(card=>{
                const tags = (card.dataset.tags || '').toLowerCase();
                const ok = filter==='all' || tags.includes(filter.toLowerCase());
                card.style.display = ok ? '' : 'none';
            });
            // Reset scroll position and trigger pagination update after filter
            setTimeout(() => {
                if (latestContainer) {
                    latestContainer.scrollLeft = 0;
                    const scrollEvent = new Event('scroll');
                    latestContainer.dispatchEvent(scrollEvent);
                    // Also trigger update manually after a short delay
                    setTimeout(() => {
                        latestContainer.dispatchEvent(scrollEvent);
                    }, 100);
                }
            }, 150);
        });
    });
    
    // Get all cards (including hidden ones for filter)
    const cards = Array.from(latestContainer.querySelectorAll('.event-card-simple'));
    if (cards.length === 0) return;
    
    function getVisibleCards() {
        return cards.filter(card => card.offsetParent !== null);
    }
    
    function getCardWidth() {
        const visibleCards = getVisibleCards();
        if (visibleCards.length === 0) return 0;
        const card = visibleCards[0];
        const gap = 16; // gap from CSS
        return card.offsetWidth + gap;
    }
    
    function getCardsPerView() {
        const cardWidth = getCardWidth();
        if (cardWidth === 0) return 1;
        const containerWidth = latestContainer.offsetWidth;
        return Math.max(1, Math.floor(containerWidth / cardWidth));
    }
    
    function getTotalPages() {
        const visibleCards = getVisibleCards();
        const cardsPerView = getCardsPerView();
        return Math.max(1, Math.ceil(visibleCards.length / cardsPerView));
    }
    
    let currentPage = 1;
    
    function updatePagination() {
        const visibleCards = getVisibleCards();
        const totalPages = getTotalPages();
        const cardsPerView = getCardsPerView();
        const scrollLeft = latestContainer.scrollLeft;
        const containerWidth = latestContainer.offsetWidth;
        
        // Calculate current page based on scroll position
        // Use a threshold to handle rounding errors and ensure we're on the right page
        if (containerWidth > 0) {
            const pageFromScroll = Math.round(scrollLeft / containerWidth);
            currentPage = Math.min(Math.max(1, pageFromScroll + 1), totalPages);
        } else {
            currentPage = 1;
        }
        
        paginationInfo.textContent = `${currentPage} of ${totalPages}`;
        // Enable Previous if scrollLeft > 0 (we've scrolled) OR if we're not on page 1
        prevBtn.disabled = scrollLeft <= 5 && currentPage === 1;
        nextBtn.disabled = currentPage === totalPages || visibleCards.length <= cardsPerView;
    }
    
    function scrollToPage(page) {
        const totalPages = getTotalPages();
        if (page < 1 || page > totalPages) return;
        
        const containerWidth = latestContainer.offsetWidth;
        const targetScroll = (page - 1) * containerWidth;
        
        // Try scrollTo first, fallback to scrollLeft
        if (latestContainer.scrollTo) {
            latestContainer.scrollTo({
                left: targetScroll,
                behavior: 'smooth'
            });
        } else {
            latestContainer.scrollLeft = targetScroll;
        }
        
        // Update after scroll completes
        setTimeout(() => {
            updatePagination();
        }, 350);
    }
    
    prevBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const visibleCards = getVisibleCards();
        const totalPages = getTotalPages();
        const cardsPerView = getCardsPerView();
        
        // Force update pagination first to get correct currentPage
        updatePagination();
        
        if (currentPage > 1 && visibleCards.length > cardsPerView) {
            scrollToPage(currentPage - 1);
        }
    });
    
    nextBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const visibleCards = getVisibleCards();
        const totalPages = getTotalPages();
        const cardsPerView = getCardsPerView();
        
        // Force update pagination first to get correct currentPage
        updatePagination();
        
        if (currentPage < totalPages && visibleCards.length > cardsPerView) {
            scrollToPage(currentPage + 1);
        }
    });
    
    // Use throttled scroll event to update pagination
    let scrollTimer;
    latestContainer.addEventListener('scroll', () => {
        clearTimeout(scrollTimer);
        scrollTimer = setTimeout(() => {
            updatePagination();
        }, 50);
    });
    
    // Update on window resize
    let resizeTimer;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            updatePagination();
        }, 250);
    });
    
    // Initialize
    setTimeout(updatePagination, 100);
    
    // Also update after a short delay to catch any delayed scroll completion
    setTimeout(updatePagination, 500);
})();

// Live Events Horizontal Scroll - Copy exact logic from Latest Events
(function(){
    const liveContainer = document.querySelector('.live-events-container');
    const prevBtn = document.getElementById('prevLiveBtn');
    const nextBtn = document.getElementById('nextLiveBtn');
    const paginationInfo = document.getElementById('livePaginationInfo');
    
    if (!liveContainer || !prevBtn || !nextBtn || !paginationInfo) return;
    
    // Get all cards (including hidden ones for filter)
    const cards = Array.from(liveContainer.querySelectorAll('.event-card-simple'));
    if (cards.length === 0) return;
    
    function getVisibleCards() {
        return cards.filter(card => card.offsetParent !== null);
    }
    
    function getCardWidth() {
        const visibleCards = getVisibleCards();
        if (visibleCards.length === 0) return 0;
        const card = visibleCards[0];
        const gap = 16; // gap from CSS
        return card.offsetWidth + gap;
    }
    
    function getCardsPerView() {
        const cardWidth = getCardWidth();
        if (cardWidth === 0) return 1;
        const containerWidth = liveContainer.offsetWidth;
        return Math.max(1, Math.floor(containerWidth / cardWidth));
    }
    
    function getTotalPages() {
        const visibleCards = getVisibleCards();
        const cardsPerView = getCardsPerView();
        return Math.max(1, Math.ceil(visibleCards.length / cardsPerView));
    }
    
    let currentPage = 1;
    
    function updatePagination() {
        const visibleCards = getVisibleCards();
        const totalPages = getTotalPages();
        const cardsPerView = getCardsPerView();
        const scrollLeft = liveContainer.scrollLeft;
        const containerWidth = liveContainer.offsetWidth;
        
        // Calculate current page based on scroll position
        // Use a threshold to handle rounding errors and ensure we're on the right page
        if (containerWidth > 0) {
            const pageFromScroll = Math.round(scrollLeft / containerWidth);
            currentPage = Math.min(Math.max(1, pageFromScroll + 1), totalPages);
        } else {
            currentPage = 1;
        }
        
        paginationInfo.textContent = `${currentPage} of ${totalPages}`;
        // Enable Previous if scrollLeft > 0 (we've scrolled) OR if we're not on page 1
        prevBtn.disabled = scrollLeft <= 5 && currentPage === 1;
        nextBtn.disabled = currentPage === totalPages || visibleCards.length <= cardsPerView;
    }
    
    function scrollToPage(page) {
        const totalPages = getTotalPages();
        if (page < 1 || page > totalPages) return;
        
        const containerWidth = liveContainer.offsetWidth;
        const targetScroll = (page - 1) * containerWidth;
        
        // Update currentPage immediately for button states
        currentPage = page;
        updatePagination();
        
        // Try scrollTo first, fallback to scrollLeft
        if (liveContainer.scrollTo) {
            liveContainer.scrollTo({
                left: targetScroll,
                behavior: 'smooth'
            });
        } else {
            liveContainer.scrollLeft = targetScroll;
        }
        
        // Update after scroll completes
        setTimeout(() => {
            updatePagination();
        }, 350);
    }
    
    prevBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const visibleCards = getVisibleCards();
        const totalPages = getTotalPages();
        const cardsPerView = getCardsPerView();
        
        // Force update pagination first to get correct currentPage
        updatePagination();
        
        if (currentPage > 1 && visibleCards.length > cardsPerView) {
            scrollToPage(currentPage - 1);
        }
    });
    
    nextBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const visibleCards = getVisibleCards();
        const totalPages = getTotalPages();
        const cardsPerView = getCardsPerView();
        
        // Force update pagination first to get correct currentPage
        updatePagination();
        
        if (currentPage < totalPages && visibleCards.length > cardsPerView) {
            scrollToPage(currentPage + 1);
        }
    });
    
    // Use throttled scroll event to update pagination
    let scrollTimer;
    liveContainer.addEventListener('scroll', () => {
        clearTimeout(scrollTimer);
        scrollTimer = setTimeout(() => {
            updatePagination();
        }, 50);
    });
    
    // Update on window resize
    let resizeTimer;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            updatePagination();
        }, 250);
    });
    
    // Initialize
    setTimeout(updatePagination, 100);
    
    // Also update after a short delay to catch any delayed scroll completion
    setTimeout(updatePagination, 500);
})();
