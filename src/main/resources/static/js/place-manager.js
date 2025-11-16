/**
 * Place Manager - Manages places in temporary memory
 * Only saves to database when "Save Changes" button is clicked
 */
class PlaceManager {
    constructor() {
        this.places = [];
        this.originalPlaces = [];
        this.initializeEventListeners();
    }

    /**
     * Initialize places list from server
     * @param {Array} serverPlaces - Places list from server
     */
    initializePlaces(serverPlaces) {
        console.log('🔍 PlaceManager: Initializing places with server data:', serverPlaces);
        console.log('🔍 PlaceManager: serverPlaces type:', typeof serverPlaces);
        console.log('🔍 PlaceManager: serverPlaces length:', serverPlaces ? serverPlaces.length : 'null/undefined');
        
        this.originalPlaces = serverPlaces ? [...serverPlaces] : [];
        this.places = serverPlaces ? [...serverPlaces] : [];
        
        console.log('🔍 PlaceManager: After initialization - places:', this.places);
        console.log('🔍 PlaceManager: About to render places...');
        
        this.renderPlaces();
        
        console.log('🔍 PlaceManager: Places rendered successfully');
        console.log('🔍 PlaceManager: placeManager instance:', this);
        console.log('🔍 PlaceManager: window.placeManager:', window.placeManager);
    }

    /**
     * Initialize event listeners
     */
    initializeEventListeners() {
        // Add place button
        const addPlaceBtn = document.getElementById('addPlaceBtn');
        if (addPlaceBtn) {
            addPlaceBtn.addEventListener('click', () => this.showAddPlaceModal());
            console.log('🔍 PlaceManager: addPlaceBtn event listener attached');
        } else {
            console.log('🔍 PlaceManager: addPlaceBtn not found - likely not on update-event page');
        }

        // Modal buttons
        const savePlaceBtn = document.getElementById('savePlaceBtn');
        const closePlaceModalBtn = document.getElementById('closePlaceModalBtn');
        
        if (savePlaceBtn) {
            savePlaceBtn.addEventListener('click', () => this.addPlace());
        }
        
        if (closePlaceModalBtn) {
            closePlaceModalBtn.addEventListener('click', () => this.closeModal());
        }

        // Note: Form submission is handled by update-event.js
        // No need to add another event listener here
    }

    /**
     * Show add place modal
     */
    showAddPlaceModal() {
        const modal = document.getElementById('addPlaceModal');
        if (modal) {
            modal.style.display = 'flex';
            document.getElementById('newPlaceName').value = '';
            document.getElementById('newPlaceBuilding').value = 'NONE';
        }
    }

    /**
     * Close modal
     */
    closeModal() {
        const modal = document.getElementById('addPlaceModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    /**
     * Add new place to temporary memory
     */
    addPlace() {
        console.log('🔍 PlaceManager: addPlace() called');
        console.log('🔍 PlaceManager: this.places before add:', this.places);
        
        const name = document.getElementById('newPlaceName').value.trim();
        const building = document.getElementById('newPlaceBuilding').value;
        
        console.log('🔍 PlaceManager: name:', name, 'building:', building);
        
        if (!name) {
            alert('Please enter location name');
            return;
        }
        
        // Check for duplicate names (case insensitive)
        if (this.places.some(p => p.placeName.toLowerCase() === name.toLowerCase())) {
            alert('Location already exists');
            return;
        }

        // Add new place to temporary memory
        const newPlace = { 
            id: null, 
            placeName: name, 
            building: building, 
            isNew: true,
            isDeleted: false
        };
        
        console.log('🔍 PlaceManager: newPlace created:', newPlace);
        
        this.places.push(newPlace);
        console.log('🔍 PlaceManager: this.places after add:', this.places);
        
        this.renderPlaces();
        this.closeModal();
        
        console.log('Added new place:', newPlace);
        console.log('Current places:', this.places);
    }

    /**
     * Remove place from temporary memory
     * @param {number} index - Index of place to remove
     */
    removePlace(index) {
        const place = this.places[index];
        if (confirm(`Delete location "${place.placeName}"?`)) {
            // If it's a new place (not saved to DB yet), remove completely
            if (place.isNew) {
                this.places.splice(index, 1);
            } else {
                // If it's an existing place, mark as deleted
                place.isDeleted = true;
            }
            this.renderPlaces();
            console.log('Removed place:', place);
            console.log('Current places:', this.places);
        }
    }

    /**
     * Render places list to UI
     */
    renderPlaces() {
        console.log('🔍 PlaceManager: renderPlaces() called');
        console.log('🔍 PlaceManager: this.places:', this.places);
        console.log('🔍 PlaceManager: this.places.length:', this.places.length);
        
        const list = document.getElementById('placeList');
        console.log('🔍 PlaceManager: placeList element:', list);
        
        if (!list) {
            console.error('❌ PlaceManager: placeList element not found!');
            return;
        }
        
        list.innerHTML = '';
        
        if (this.places.length === 0) {
            console.log('🔍 PlaceManager: No places to render, showing empty message');
            list.innerHTML = '<tr><td colspan="3" style="text-align:center;color:gray;">No locations yet</td></tr>';
            return;
        }
        
        console.log('🔍 PlaceManager: Rendering', this.places.length, 'places');
        
        this.places.forEach((place, index) => {
            // Skip deleted places in rendering
            if (place.isDeleted) {
                console.log('🔍 PlaceManager: Skipping deleted place:', place);
                return;
            }
            
            console.log('🔍 PlaceManager: Rendering place', index, ':', place);
            
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${place.placeName}</td>
                <td>${place.building}</td>
                <td>
                    <button type="button" class="btn btn-danger btn-sm" onclick="placeManager.removePlace(${index})">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </td>
            `;
            list.appendChild(row);
        });
        
        console.log('🔍 PlaceManager: Finished rendering places');
    }

    /**
     * Get places data to submit form
     * @returns {Array} All places list (including deleted places)
     */
    getPlacesData() {
        return this.places; // Send all places, including deleted ones
    }

    /**
     * Get summary of changes
     * @returns {Object} Change summary
     */
    getChangesSummary() {
        const newPlaces = this.places.filter(p => p.isNew && !p.isDeleted);
        const deletedPlaces = this.places.filter(p => p.isDeleted && !p.isNew);
        const existingPlaces = this.places.filter(p => !p.isNew && !p.isDeleted);
        
        return {
            newPlaces,
            deletedPlaces,
            existingPlaces,
            totalChanges: newPlaces.length + deletedPlaces.length
        };
    }
}

// Global place manager instance
let placeManager = null;

/**
 * Initialize PlaceManager with data from server
 * @param {Array} serverPlaces - Places data from server
 */
function initializePlaceManager(serverPlaces) {
    console.log('🔍 initializePlaceManager() called');
    console.log('🔍 initializePlaceManager: serverPlaces:', serverPlaces);
    console.log('🔍 initializePlaceManager: serverPlaces type:', typeof serverPlaces);
    console.log('🔍 initializePlaceManager: serverPlaces length:', serverPlaces ? serverPlaces.length : 'null/undefined');
    
    if (!placeManager) {
        console.log('🔍 initializePlaceManager: Creating new PlaceManager instance');
        placeManager = new PlaceManager();
    } else {
        console.log('🔍 initializePlaceManager: Using existing PlaceManager instance');
    }
    
    console.log('🔍 initializePlaceManager: About to call placeManager.initializePlaces()');
    placeManager.initializePlaces(serverPlaces || []);
    console.log('🔍 initializePlaceManager: Completed successfully');
}

/**
 * Populate places from database (similar to populateLineupFromEvent and populateSchedulesFromEvent)
 */
function populatePlacesFromEvent() {
    console.log('--- STEP B: populatePlacesFromEvent RUNNING ---');
    console.log('initialPlacesData:', initialPlacesData);
    console.log('typeof initialPlacesData:', typeof initialPlacesData);
    console.log('window.placeManager before:', window.placeManager);
    
    // 1. Check data
    if (!initialPlacesData || initialPlacesData.length === 0) {
        console.log('No initial places to populate.');
        // Still initialize PlaceManager with empty data
        if (!placeManager) {
            console.log('Creating new PlaceManager instance with empty data...');
            placeManager = new PlaceManager();
            window.placeManager = placeManager; // Ensure it's set globally
        }
        placeManager.initializePlaces([]);
        console.log('window.placeManager after empty init:', window.placeManager);
        return;
    }
    console.log('LOG: Found', initialPlacesData.length, 'places to load.');

    // 2. Initialize PlaceManager with data from database
    if (!placeManager) {
        console.log('Creating new PlaceManager instance...');
        placeManager = new PlaceManager();
        window.placeManager = placeManager; // Ensure it's set globally
    }
    
    console.log('Initializing PlaceManager with database data...');
    placeManager.initializePlaces(initialPlacesData);
    
    console.log('✅ Places populated successfully from database');
    console.log('window.placeManager after populated:', window.placeManager);
}

// Make placeManager globally accessible
window.placeManager = placeManager;
window.initializePlaceManager = initializePlaceManager;
window.populatePlacesFromEvent = populatePlacesFromEvent;