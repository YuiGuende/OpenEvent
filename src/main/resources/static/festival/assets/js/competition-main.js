/* Competition Main JavaScript - Custom functionality for competition theme */

(function($) {
    'use strict';

    // Competition theme initialization
    $(document).ready(function() {
        
        // Initialize competition-specific features
        initCompetitionTheme();
        initCompetitionSlider();
        initCompetitionCountdown();
        initCompetitionAnimations();
        initCompetitionForms();
        initCompetitionGallery();
        
    });

    // Competition theme initialization
    function initCompetitionTheme() {
        console.log('Competition theme initialized');
        
        // Add competition-specific body class
        $('body').addClass('competition-theme');
        
        // Initialize tooltips for competition elements
        $('[data-toggle="tooltip"]').tooltip();
        
        // Add competition-specific event listeners
        addCompetitionEventListeners();
    }

    // Competition slider functionality
    function initCompetitionSlider() {
        const heroTrack = document.getElementById('heroTrack');
        const heroSlides = document.querySelectorAll('.hero-slide');
        const prevBtn = document.getElementById('heroPrev');
        const nextBtn = document.getElementById('heroNext');
        const progressBars = document.querySelectorAll('.hero-progress-bar');

        if (!heroTrack || !heroSlides.length) return;

        let currentSlide = 0;
        const totalSlides = heroSlides.length;
        let autoSlideInterval;

        function updateSlider() {
            const translateX = -currentSlide * 100;
            heroTrack.style.transform = `translateX(${translateX}%)`;

            // Update progress bars with competition styling
            progressBars.forEach((bar, index) => {
                bar.classList.toggle('active', index === currentSlide);
                
                // Add competition-specific animation
                if (index === currentSlide) {
                    bar.style.background = 'linear-gradient(90deg, #ffd700, #ff6a00)';
                } else {
                    bar.style.background = 'rgba(255, 255, 255, 0.3)';
                }
            });

            // Add slide transition effect
            heroTrack.style.transition = 'transform 0.5s cubic-bezier(0.4, 0, 0.2, 1)';
        }

        function nextSlide() {
            currentSlide = (currentSlide + 1) % totalSlides;
            updateSlider();
            restartAutoSlide();
        }

        function prevSlide() {
            currentSlide = (currentSlide - 1 + totalSlides) % totalSlides;
            updateSlider();
            restartAutoSlide();
        }

        function startAutoSlide() {
            autoSlideInterval = setInterval(nextSlide, 6000); // 6 seconds for competition
        }

        function stopAutoSlide() {
            if (autoSlideInterval) {
                clearInterval(autoSlideInterval);
            }
        }

        function restartAutoSlide() {
            stopAutoSlide();
            startAutoSlide();
        }

        // Event listeners with competition-specific enhancements
        if (nextBtn) {
            nextBtn.addEventListener('click', function() {
                nextSlide();
                // Add click animation
                this.style.transform = 'translateY(-50%) scale(0.95)';
                setTimeout(() => {
                    this.style.transform = 'translateY(-50%) scale(1)';
                }, 150);
            });
        }

        if (prevBtn) {
            prevBtn.addEventListener('click', function() {
                prevSlide();
                // Add click animation
                this.style.transform = 'translateY(-50%) scale(0.95)';
                setTimeout(() => {
                    this.style.transform = 'translateY(-50%) scale(1)';
                }, 150);
            });
        }

        // Progress bar click handlers with competition effects
        progressBars.forEach((bar, index) => {
            bar.addEventListener('click', function() {
                currentSlide = index;
                updateSlider();
                restartAutoSlide();
                
                // Add click effect
                this.style.transform = 'scale(1.2)';
                setTimeout(() => {
                    this.style.transform = 'scale(1)';
                }, 200);
            });
        });

        // Pause auto-slide on hover
        heroTrack.addEventListener('mouseenter', stopAutoSlide);
        heroTrack.addEventListener('mouseleave', startAutoSlide);

        // Initialize
        updateSlider();
        startAutoSlide();

        // Add competition-specific keyboard navigation
        document.addEventListener('keydown', function(e) {
            if (e.key === 'ArrowLeft') {
                prevSlide();
            } else if (e.key === 'ArrowRight') {
                nextSlide();
            }
        });
    }

    // Competition countdown functionality
    function initCompetitionCountdown() {
        const clockElement = document.getElementById("clock");
        if (!clockElement) return;

        // Get event start date from Thymeleaf
        const eventStartDateStr = clockElement.getAttribute('data-start-date') || '';
        const eventStartDate = eventStartDateStr ? new Date(eventStartDateStr).getTime() : null;

        if (!eventStartDate) {
            clockElement.innerHTML = "<div class='alert alert-warning'>Competition date not available</div>";
            return;
        }

        const countdown = setInterval(function () {
            const now = new Date().getTime();
            const distance = eventStartDate - now;

            if (distance < 0) {
                clearInterval(countdown);
                clockElement.innerHTML = `
                    <div class="time-entry competition-active">
                        <span>🏆</span>
                        <div class="label">COMPETITION IS LIVE!</div>
                    </div>
                `;
                return;
            }

            const days = Math.floor(distance / (1000 * 60 * 60 * 24));
            const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((distance % (1000 * 60)) / 1000);

            // Add competition-specific styling
            clockElement.innerHTML = `
                <div class="time-entry">
                    <span>${days}</span>
                    <div class="label">DAYS</div>
                </div>
                <div class="time-entry">
                    <span>${hours.toString().padStart(2, '0')}</span>
                    <div class="label">HOURS</div>
                </div>
                <div class="time-entry">
                    <span>${minutes.toString().padStart(2, '0')}</span>
                    <div class="label">MINUTES</div>
                </div>
                <div class="time-entry">
                    <span>${seconds.toString().padStart(2, '0')}</span>
                    <div class="label">SECONDS</div>
                </div>
            `;

            // Add pulsing effect for last minute
            if (days === 0 && hours === 0 && minutes <= 5) {
                clockElement.classList.add('countdown-urgent');
            } else {
                clockElement.classList.remove('countdown-urgent');
            }

        }, 1000);
    }

    // Competition animations
    function initCompetitionAnimations() {
        // Animate competition badges
        $('.competition-badge').each(function(index) {
            $(this).css({
                'animation-delay': (index * 0.2) + 's'
            });
        });

        // Animate judge cards on scroll
        $(window).scroll(function() {
            $('.judge-card').each(function() {
                const elementTop = $(this).offset().top;
                const elementBottom = elementTop + $(this).outerHeight();
                const viewportTop = $(window).scrollTop();
                const viewportBottom = viewportTop + $(window).height();

                if (elementBottom > viewportTop && elementTop < viewportBottom) {
                    $(this).addClass('animate-in');
                }
            });
        });

        // Add hover effects to competition elements
        $('.prize-section').hover(
            function() {
                $(this).addClass('prize-hover');
            },
            function() {
                $(this).removeClass('prize-hover');
            }
        );

        // Competition button animations
        $('.btn-common').on('mouseenter', function() {
            $(this).addClass('btn-competition-hover');
        }).on('mouseleave', function() {
            $(this).removeClass('btn-competition-hover');
        });
    }

    // Competition forms
    function initCompetitionForms() {
        // Registration form validation
        $('#competition-registration-form').on('submit', function(e) {
            e.preventDefault();
            
            const form = $(this);
            const submitBtn = form.find('button[type="submit"]');
            const originalText = submitBtn.text();
            
            // Add loading state
            submitBtn.prop('disabled', true).html('<i class="lni-spinner lni-spin"></i> Registering...');
            
            // Simulate form submission (replace with actual AJAX call)
            setTimeout(function() {
                submitBtn.prop('disabled', false).text(originalText);
                
                // Show success message
                showCompetitionNotification('Registration successful!', 'success');
            }, 2000);
        });

        // Email subscription
        $('#competition-subscribe-form').on('submit', function(e) {
            e.preventDefault();
            
            const email = $(this).find('input[name="email"]').val();
            if (validateEmail(email)) {
                showCompetitionNotification('Thank you for subscribing!', 'success');
                $(this)[0].reset();
            } else {
                showCompetitionNotification('Please enter a valid email address', 'error');
            }
        });
    }

    // Competition gallery
    function initCompetitionGallery() {
        // Initialize lightbox for competition gallery
        if (typeof $.fn.nivoLightbox !== 'undefined') {
            $('.gallery-box .lightbox').nivoLightbox({
                effect: 'fadeScale',
                theme: 'default',
                keyboardNav: true,
                clickOverlayToClose: true,
                onShowLightbox: function() {
                    console.log('Competition gallery opened');
                },
                onHideLightbox: function() {
                    console.log('Competition gallery closed');
                }
            });
        }

        // Add gallery hover effects
        $('.gallery-box').on('mouseenter', function() {
            $(this).find('.overlay-box').addClass('gallery-overlay-hover');
        }).on('mouseleave', function() {
            $(this).find('.overlay-box').removeClass('gallery-overlay-hover');
        });
    }

    // Competition event listeners
    function addCompetitionEventListeners() {
        // Back to top button
        $(window).scroll(function() {
            if ($(this).scrollTop() > 300) {
                $('.back-to-top').fadeIn();
            } else {
                $('.back-to-top').fadeOut();
            }
        });

        $('.back-to-top').on('click', function(e) {
            e.preventDefault();
            $('html, body').animate({
                scrollTop: 0
            }, 800, 'easeInOutCubic');
        });

        // Competition FAQ accordion
        $('.accordion .header-title').on('click', function() {
            const target = $(this).data('target');
            const collapse = $(target);
            
            // Close other accordions
            $('.accordion .collapse').not(collapse).collapse('hide');
            
            // Toggle current accordion
            collapse.collapse('toggle');
            
            // Add competition-specific animation
            $(this).addClass('accordion-clicked');
            setTimeout(() => {
                $(this).removeClass('accordion-clicked');
            }, 300);
        });

        // Competition schedule tabs
        $('.schedule-tab-title .nav-link').on('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all tabs
            $('.schedule-tab-title .nav-link').removeClass('active');
            
            // Add active class to clicked tab
            $(this).addClass('active');
            
            // Show corresponding content
            const target = $(this).attr('href');
            $('.tab-content .tab-pane').removeClass('show active');
            $(target).addClass('show active');
        });
    }

    // Competition notification system
    function showCompetitionNotification(message, type = 'info') {
        const notification = $(`
            <div class="competition-notification competition-notification-${type}">
                <div class="notification-content">
                    <i class="lni-${type === 'success' ? 'check-mark-circle' : type === 'error' ? 'close' : 'info'}"></i>
                    <span>${message}</span>
                </div>
                <button class="notification-close">&times;</button>
            </div>
        `);

        $('body').append(notification);

        // Animate in
        notification.addClass('notification-show');

        // Auto remove after 5 seconds
        setTimeout(() => {
            notification.removeClass('notification-show');
            setTimeout(() => {
                notification.remove();
            }, 300);
        }, 5000);

        // Manual close
        notification.find('.notification-close').on('click', function() {
            notification.removeClass('notification-show');
            setTimeout(() => {
                notification.remove();
            }, 300);
        });
    }

    // Utility functions
    function validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    // Competition-specific CSS additions
    function addCompetitionStyles() {
        const styles = `
            <style>
                .competition-notification {
                    position: fixed;
                    top: 20px;
                    right: 20px;
                    background: white;
                    border-radius: 10px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                    padding: 15px 20px;
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    z-index: 9999;
                    transform: translateX(400px);
                    transition: transform 0.3s ease;
                    border-left: 4px solid #ff6a00;
                }
                
                .competition-notification-show {
                    transform: translateX(0);
                }
                
                .competition-notification-success {
                    border-left-color: #27ae60;
                }
                
                .competition-notification-error {
                    border-left-color: #e74c3c;
                }
                
                .notification-content {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    color: #333;
                    font-weight: 500;
                }
                
                .notification-close {
                    background: none;
                    border: none;
                    font-size: 18px;
                    cursor: pointer;
                    color: #666;
                    padding: 0;
                    margin-left: 10px;
                }
                
                .countdown-urgent .time-entry {
                    animation: pulse 1s infinite;
                }
                
                .btn-competition-hover {
                    transform: translateY(-3px) scale(1.05);
                }
                
                .prize-hover {
                    transform: scale(1.02);
                    box-shadow: 0 15px 50px rgba(255, 215, 0, 0.4);
                }
                
                .gallery-overlay-hover {
                    opacity: 1 !important;
                }
                
                .accordion-clicked {
                    transform: translateX(5px);
                    transition: transform 0.3s ease;
                }
                
                .judge-card.animate-in {
                    animation: slideInUp 0.6s ease forwards;
                }
                
                @keyframes slideInUp {
                    from {
                        opacity: 0;
                        transform: translateY(30px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
            </style>
        `;
        
        $('head').append(styles);
    }

    // Initialize additional styles
    addCompetitionStyles();

})(jQuery);
