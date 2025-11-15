// Dashboard Statistics Calculator
class DashboardStats {
    constructor(statsData) {
        this.statsData = statsData
        this.charts = [] // Lưu trữ các chart instances để có thể resize
        this.init().then(() => {
            console.log('🎯 Dashboard initialization completed')
        })
    }

    async init() {
        this.renderKPICards()
        await this.renderCharts()
        this.renderTicketDetailsTable()
        this.setupResizeListener()
    }

    setupResizeListener() {
        // Lắng nghe sự kiện resize window
        let resizeTimeout;
        window.addEventListener('resize', () => {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(() => {
                this.resizeCharts();
            }, 250); // Debounce để tránh resize quá nhiều lần
        });
        
        // Lắng nghe sự kiện sidebar toggle (nếu có)
        const sidebarToggle = document.querySelector('[data-toggle="sidebar"]') || 
                             document.querySelector('.sidebar-toggle') ||
                             document.querySelector('#sidebarToggle');
        
        if (sidebarToggle) {
            sidebarToggle.addEventListener('click', () => {
                setTimeout(() => {
                    this.resizeCharts();
                }, 350); // Delay để sidebar animation hoàn thành
            });
        }
        
        // Lắng nghe sự kiện khi sidebar được toggle bằng CSS classes
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes' && 
                    (mutation.attributeName === 'class' || mutation.attributeName === 'style')) {
                    setTimeout(() => {
                        this.resizeCharts();
                    }, 100);
                }
            });
        });
        
        // Observe body và main container để detect sidebar changes
        const body = document.body;
        const mainContainer = document.querySelector('.main-content') || 
                             document.querySelector('#main-content') ||
                             document.querySelector('.content-wrapper');
        
        if (body) observer.observe(body, { attributes: true });
        if (mainContainer) observer.observe(mainContainer, { attributes: true });
        
        // Lắng nghe sự kiện khi window được maximize/restore
        window.addEventListener('maximize', () => this.resizeCharts());
        window.addEventListener('restore', () => this.resizeCharts());
        
        console.log('✅ Resize listeners setup completed');
    }

    resizeCharts() {
        console.log('🔄 Resizing charts...', this.charts.length, 'charts found');
        
        if (this.charts.length === 0) {
            console.warn('⚠️ No charts to resize');
            return;
        }
        
        let resizedCount = 0;
        this.charts.forEach((chart, index) => {
            if (chart && typeof chart.resize === 'function') {
                try {
                    chart.resize();
                    resizedCount++;
                    console.log(`✅ Chart ${index + 1} resized successfully`);
                } catch (error) {
                    console.error(`❌ Error resizing chart ${index + 1}:`, error);
                }
            } else {
                console.warn(`⚠️ Chart ${index + 1} is invalid or has no resize method`);
            }
        });
        
        console.log(`🎯 Resize completed: ${resizedCount}/${this.charts.length} charts resized`);
    }

    // Method để cleanup charts khi destroy dashboard
    destroyCharts() {
        this.charts.forEach(chart => {
            if (chart && typeof chart.destroy === 'function') {
                chart.destroy()
            }
        })
        this.charts = []
    }

    // Method để force render lại check-in chart
    forceRenderCheckInChart() {
        console.log('🔄 Force rendering Check-in Chart...')
        const chartElement = document.querySelector("#checkInPieChart")
        if (chartElement) {
            chartElement.innerHTML = '' // Clear content
        }
        this.renderCheckInPieChart()
    }

    // Removed calculateStats() method - now using data from API

    renderKPICards() {
        console.log('🎯 Rendering KPI Cards with data:', this.statsData)
        
        // Debug từng giá trị trước khi render
        console.log('📊 KPI Values before rendering:')
        console.log('  - totalRevenue:', this.statsData.totalRevenue, typeof this.statsData.totalRevenue)
        console.log('  - totalTicketsSold:', this.statsData.totalTicketsSold, typeof this.statsData.totalTicketsSold)
        console.log('  - totalAttendees:', this.statsData.totalAttendees, typeof this.statsData.totalAttendees)
        console.log('  - refundRate:', this.statsData.refundRate, typeof this.statsData.refundRate)
        console.log('  - unsoldRate:', this.statsData.unsoldRate, typeof this.statsData.unsoldRate)
        
        // Cập nhật KPI Cards từ API data
        document.getElementById("totalRevenue").textContent = this.formatCurrency(this.statsData.totalRevenue)
        document.getElementById("totalTicketsSold").textContent = this.statsData.totalTicketsSold
        document.getElementById("totalAttendees").textContent = this.statsData.totalAttendees
        document.getElementById("refundRate").textContent = this.statsData.refundRate.toFixed(1) + "%"
        document.getElementById("unsoldRate").textContent = this.statsData.unsoldRate.toFixed(1) + "%"
        
        // Cập nhật voucher data nếu có
        if (this.statsData.vouchersUsed !== undefined) {
            document.getElementById("vouchersUsed").textContent = this.statsData.vouchersUsed
        }
        
        console.log('✅ KPI Cards rendered successfully')
    }

    async renderCharts() {
        // Delay một chút để đảm bảo DOM đã render xong
        setTimeout(async () => {
            console.log('🎨 Starting to render all charts...')
            
            // Render tất cả charts và đợi chúng hoàn thành
            await Promise.all([
                this.renderCheckInPieChart(),
                this.renderCheckInBarChart(),
                this.renderRevenueByTypeChart(),
                this.renderRevenueBarChart(),
                this.renderComboChart(),
                this.renderAreaChart()
            ])
            
            console.log('✅ All charts rendered, total charts:', this.charts.length)
            console.log('📊 Charts array:', this.charts)
        }, 100)
    }

    async renderCheckInPieChart() {
        console.log('🎯 Rendering Check-in Pie Chart...')
        console.log('📊 Stats Data:', this.statsData)
        
        // Cập nhật thông tin check-in từ API data
        document.getElementById("checkInPercentage").textContent = this.statsData.checkInRate.toFixed(1) + "%"
        document.getElementById("checkInCount").textContent = this.statsData.totalCheckIn
        document.getElementById("totalTicketsForCheckIn").textContent = this.statsData.totalTicketsSold

        // Kiểm tra xem ApexCharts có sẵn không
        if (typeof ApexCharts === 'undefined') {
            console.error('❌ ApexCharts is not loaded!')
            return
        }

        // Tạo dữ liệu cho pie chart check-in
        const checkInData = this.statsData.totalCheckIn || 0
        const notCheckInData = (this.statsData.totalTicketsSold || 0) - checkInData
        
        // Nếu không có dữ liệu, không render chart
        if (checkInData === 0 && notCheckInData === 0) {
            console.log('⚠️ No check-in data available, skipping chart render')
            const chartElement = document.querySelector("#checkInPieChart")
            if (chartElement) {
                chartElement.innerHTML = '<div class="text-center text-muted p-4">Chưa có dữ liệu check-in</div>'
            }
            return
        }
        
        const finalCheckInData = checkInData
        const finalNotCheckInData = notCheckInData
        
        console.log('📈 Chart Data:', { 
            original: { checkInData, notCheckInData },
            final: { finalCheckInData, finalNotCheckInData }
        })

        // Kiểm tra element tồn tại
        const chartElement = document.querySelector("#checkInPieChart")
        if (!chartElement) {
            console.error('❌ Chart element #checkInPieChart not found!')
            return
        }
        
        console.log('✅ Chart element found:', chartElement)

        const options = {
            series: [finalCheckInData, finalNotCheckInData],
            chart: {
                width: 350,
                type: 'pie',
                responsive: [{
                    breakpoint: 768,
                    options: {
                        width: 300
                    }
                }, {
                    breakpoint: 480,
            options: {
                        width: 250
                    }
                }]
            },
            labels: ['Đã Check-in', 'Chưa Check-in'],
            colors: ['#10B981', '#E5E7EB'],
                    legend: {
                position: 'bottom',
                fontSize: '12px',
                fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
                markers: {
                    width: 8,
                    height: 8,
                    radius: 2
                }
                    },
                    tooltip: {
                y: {
                    formatter: function (val) {
                        return val + " vé"
                    }
                }
            },
            dataLabels: {
                enabled: true,
                formatter: function (val, opts) {
                    return opts.w.config.series[opts.seriesIndex] + " vé"
                }
            }
        }

        try {
            console.log('🎨 Creating ApexCharts with options:', options)
            const chart = new ApexCharts(chartElement, options)
            await chart.render()
            this.charts.push(chart) // Lưu chart instance sau khi render xong
            console.log('✅ Check-in pie chart rendered successfully, chart instance:', chart)
            console.log('📊 Chart has resize method:', typeof chart.resize === 'function')
        } catch (error) {
            console.error('❌ Error rendering check-in pie chart:', error)
        }
    }

    async renderCheckInBarChart() {
        // Kiểm tra xem ApexCharts có sẵn không
        if (typeof ApexCharts === 'undefined') {
            console.error('ApexCharts is not loaded!')
            return
        }

        // Tạo dữ liệu cho biểu đồ cột check-in theo loại vé
        const labels = []
        const checkInData = []
        const totalData = []

        this.statsData.ticketTypeStats.forEach((ticketType) => {
            labels.push(ticketType.name || "N/A")
            checkInData.push(ticketType.checkInCount)
            totalData.push(ticketType.soldQuantity)
        })

        const options = {
            series: [{
                name: 'Đã Check-in',
                data: checkInData,
                color: '#10B981'
            }, {
                name: 'Tổng Vé Bán',
                data: totalData,
                color: '#E5E7EB'
            }],
            chart: {
                type: 'bar',
                height: 300,
                toolbar: {
                    show: true
                },
                responsive: [{
                    breakpoint: 768,
                    options: {
                        height: 250
                    }
                }, {
                    breakpoint: 480,
                    options: {
                        height: 200
                    }
                }]
            },
            plotOptions: {
                bar: {
                    horizontal: false,
                    columnWidth: '55%',
                    endingShape: 'rounded'
                }
            },
            dataLabels: {
                enabled: false
            },
            stroke: {
                show: true,
                width: 2,
                colors: ['transparent']
            },
            xaxis: {
                categories: labels,
                labels: {
                    style: {
                        fontSize: '12px',
                        fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                    }
                }
            },
            yaxis: {
                title: {
                    text: 'Số Lượng Vé'
                }
            },
            fill: {
                opacity: 1
            },
            tooltip: {
                y: {
                    formatter: function (val) {
                        return val + " vé"
                    }
                }
            },
            legend: {
                position: 'top',
                horizontalAlign: 'left'
            }
        }

        try {
            const chartElement = document.querySelector("#checkInBarChart")
            if (chartElement) {
                const chart = new ApexCharts(chartElement, options)
                await chart.render()
                this.charts.push(chart) // Lưu chart instance sau khi render xong
                console.log('✅ Check-in bar chart rendered successfully, chart instance:', chart)
            } else {
                console.error('Chart element #checkInBarChart not found')
            }
        } catch (error) {
            console.error('Error rendering check-in bar chart:', error)
        }
    }

    async renderRevenueByTypeChart() {
        // Chuẩn bị dữ liệu cho pie chart từ API data
        const labels = []
        const data = []
        const colors = ["#FF9500", "#10B981", "#3B82F6", "#F59E0B", "#EC4899", "#8B5CF6"]

        this.statsData.revenueByType.forEach((revenueData) => {
            if (revenueData.revenue > 0) {
                labels.push(revenueData.ticketTypeName)
                data.push(revenueData.revenue)
            }
        })

        // Nếu không có dữ liệu, không render chart
        if (data.length === 0) {
            console.log('⚠️ No revenue data available, skipping chart render')
            const chartElement = document.querySelector("#revenueByTypeChart")
            if (chartElement) {
                chartElement.innerHTML = '<div class="text-center text-muted p-4">Chưa có dữ liệu doanh thu</div>'
            }
            return
        }

        const options = {
            series: data,
            chart: {
                width: 380,
                type: 'pie',
                responsive: [{
                    breakpoint: 768,
                    options: {
                        width: 300
                    }
                }, {
                    breakpoint: 480,
                    options: {
                        width: 250
                    }
                }]
            },
                labels: labels,
            colors: colors.slice(0, labels.length),
            legend: {
                position: 'bottom',
                fontSize: '12px',
                fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
                markers: {
                    width: 8,
                    height: 8,
                    radius: 2
                }
            },
            tooltip: {
                y: {
                    formatter: function (val) {
                        return new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: "VND",
                        }).format(val)
                    }
                }
            }
        }

        try {
            const chartElement = document.querySelector("#revenueByTypeChart")
            if (chartElement) {
                const chart = new ApexCharts(chartElement, options)
                await chart.render()
                this.charts.push(chart) // Lưu chart instance sau khi render xong
                console.log('✅ Revenue by type chart rendered successfully, chart instance:', chart)
            } else {
                console.error('Chart element #revenueByTypeChart not found')
            }
        } catch (error) {
            console.error('Error rendering revenue by type chart:', error)
        }
    }

    async renderRevenueBarChart() {
        // Kiểm tra xem ApexCharts có sẵn không
        if (typeof ApexCharts === 'undefined') {
            console.error('ApexCharts is not loaded!')
            return
        }

        // Tạo dữ liệu cho biểu đồ cột doanh thu theo loại vé
        const labels = []
        const revenueData = []

        this.statsData.revenueByType.forEach((revenueItem) => {
            if (revenueItem.revenue > 0) {
                labels.push(revenueItem.ticketTypeName)
                revenueData.push(revenueItem.revenue)
            }
        })

        // Nếu không có dữ liệu, không render chart
        if (revenueData.length === 0) {
            console.log('⚠️ No revenue data available, skipping chart render')
            const chartElement = document.querySelector("#revenueBarChart")
            if (chartElement) {
                chartElement.innerHTML = '<div class="text-center text-muted p-4">Chưa có dữ liệu doanh thu</div>'
            }
            return
        }

        const options = {
            series: [{
                name: 'Doanh Thu',
                data: revenueData,
                color: '#3B82F6'
            }],
            chart: {
                type: 'bar',
                height: 300,
                toolbar: {
                    show: true
                }
            },
            plotOptions: {
                bar: {
                    horizontal: false,
                    columnWidth: '60%',
                    endingShape: 'rounded',
                    borderRadius: 4
                }
            },
            dataLabels: {
                enabled: false
            },
            stroke: {
                show: true,
                width: 2,
                colors: ['transparent']
            },
            xaxis: {
                categories: labels,
                        labels: {
                    style: {
                        fontSize: '12px',
                        fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                    }
                }
            },
            yaxis: {
                title: {
                    text: 'Doanh Thu (VND)'
                },
                labels: {
                    formatter: function (val) {
                        return new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: "VND",
                            minimumFractionDigits: 0
                        }).format(val)
                    }
                }
            },
            fill: {
                opacity: 1,
                gradient: {
                    shade: 'light',
                    type: 'vertical',
                    shadeIntensity: 0.25,
                    gradientToColors: ['#1D4ED8'],
                    inverseColors: false,
                    opacityFrom: 1,
                    opacityTo: 1,
                    stops: [0, 100]
                }
                    },
                    tooltip: {
                y: {
                    formatter: function (val) {
                        return new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: "VND",
                        }).format(val)
                    }
                }
            },
                    legend: {
                show: false
            }
        }

        try {
            const chartElement = document.querySelector("#revenueBarChart")
            if (chartElement) {
                const chart = new ApexCharts(chartElement, options)
                await chart.render()
                this.charts.push(chart) // Lưu chart instance sau khi render xong
                console.log('✅ Revenue bar chart rendered successfully, chart instance:', chart)
            } else {
                console.error('Chart element #revenueBarChart not found')
            }
        } catch (error) {
            console.error('Error rendering revenue bar chart:', error)
        }
    }

    async renderComboChart() {
        // Kiểm tra xem ApexCharts có sẵn không
        if (typeof ApexCharts === 'undefined') {
            console.error('ApexCharts is not loaded!')
            return
        }

        // Sử dụng dữ liệu thực từ API
        let dates = []
        let revenueData = []
        let orderCountData = []
        
        if (this.statsData.dailyStats && this.statsData.dailyStats.length > 0) {
            // Sử dụng dữ liệu thực từ API
            this.statsData.dailyStats.forEach(dailyStat => {
                dates.push(dailyStat.date)
                revenueData.push(dailyStat.revenue)
                orderCountData.push(dailyStat.ordersCount)
            })
        } else {
            // Nếu không có dữ liệu, không render chart
            console.log('⚠️ No daily stats data available, skipping combo chart render')
            const chartElement = document.querySelector("#comboChart")
            if (chartElement) {
                chartElement.innerHTML = '<div class="text-center text-muted p-4">Chưa có dữ liệu thống kê theo ngày</div>'
            }
            return
        }

        const options = {
            series: [{
                name: 'Doanh Thu',
                type: 'column',
                data: revenueData
            }, {
                name: 'Số Lượng Đơn',
                type: 'line',
                data: orderCountData
            }],
            chart: {
                height: 350,
                type: 'line',
                toolbar: {
                    show: true
                }
            },
            stroke: {
                width: [0, 4]
            },
            dataLabels: {
                enabled: true,
                enabledOnSeries: [1]
            },
            labels: dates,
            xaxis: {
                type: 'category'
            },
            yaxis: [{
                title: {
                    text: 'Doanh Thu (VND)',
                },
                labels: {
                    formatter: function (val) {
                        return new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: "VND",
                            minimumFractionDigits: 0
                        }).format(val)
                    }
                }
            }, {
                opposite: true,
                title: {
                    text: 'Số Lượng Đơn'
                }
            }],
            colors: ['#3B82F6', '#10B981'],
            legend: {
                position: 'top',
                horizontalAlign: 'left'
                    },
                    tooltip: {
                shared: true,
                intersect: false,
                y: {
                    formatter: function (val, { seriesIndex }) {
                        if (seriesIndex === 0) {
                            return new Intl.NumberFormat("vi-VN", {
                                style: "currency",
                                currency: "VND",
                            }).format(val)
                        }
                        return val + ' đơn'
                    }
                }
            }
        }

        try {
            const chartElement = document.querySelector("#comboChart")
            if (chartElement) {
                const chart = new ApexCharts(chartElement, options)
                await chart.render()
                this.charts.push(chart) // Lưu chart instance sau khi render xong
                console.log('✅ Combo chart rendered successfully, chart instance:', chart)
            } else {
                console.error('Chart element #comboChart not found')
            }
        } catch (error) {
            console.error('Error rendering combo chart:', error)
        }
    }

    async renderAreaChart() {
        // Kiểm tra xem ApexCharts có sẵn không
        if (typeof ApexCharts === 'undefined') {
            console.error('ApexCharts is not loaded!')
            return
        }

        // Sử dụng dữ liệu thực từ API
        let dates = []
        let revenueData = []
        let ticketData = []
        
        if (this.statsData.dailyStats && this.statsData.dailyStats.length > 0) {
            // Sử dụng dữ liệu thực từ API
            this.statsData.dailyStats.forEach(dailyStat => {
                dates.push(dailyStat.date)
                revenueData.push(dailyStat.revenue)
                ticketData.push(dailyStat.ticketsSold)
            })
        } else {
            // Nếu không có dữ liệu, không render chart
            console.log('⚠️ No daily stats data available, skipping area chart render')
            const chartElement = document.querySelector("#areaChart")
            if (chartElement) {
                chartElement.innerHTML = '<div class="text-center text-muted p-4">Chưa có dữ liệu thống kê theo ngày</div>'
            }
            return
        }

        const options = {
            series: [{
                name: 'Doanh Thu',
                data: revenueData,
                color: '#3B82F6'
            }, {
                name: 'Vé Bán',
                data: ticketData,
                color: '#10B981'
            }],
            chart: {
                height: 300,
                type: 'area',
                toolbar: {
                    show: true
                }
            },
            dataLabels: {
                enabled: false
            },
            stroke: {
                curve: 'smooth',
                width: 3
            },
            fill: {
                type: 'gradient',
                gradient: {
                    shadeIntensity: 1,
                    opacityFrom: 0.7,
                    opacityTo: 0.3,
                    stops: [0, 90, 100]
                }
            },
            xaxis: {
                categories: dates,
                labels: {
                    style: {
                        fontSize: '12px',
                        fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                    }
                }
            },
            yaxis: [{
                title: {
                    text: 'Doanh Thu (VND)',
                },
                labels: {
                    formatter: function (val) {
                        return new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: "VND",
                            minimumFractionDigits: 0
                        }).format(val)
                    }
                }
            }, {
                opposite: true,
                title: {
                    text: 'Số Vé Bán'
                }
            }],
            legend: {
                position: 'top',
                horizontalAlign: 'left'
            },
            tooltip: {
                shared: true,
                intersect: false,
                y: {
                    formatter: function (val, { seriesIndex }) {
                        if (seriesIndex === 0) {
                            return new Intl.NumberFormat("vi-VN", {
                                style: "currency",
                                currency: "VND",
                            }).format(val)
                        }
                        return val + ' vé'
                    }
                }
            }
        }

        try {
            const chartElement = document.querySelector("#areaChart")
            if (chartElement) {
                const chart = new ApexCharts(chartElement, options)
                await chart.render()
                this.charts.push(chart) // Lưu chart instance sau khi render xong
                console.log('✅ Area chart rendered successfully, chart instance:', chart)
            } else {
                console.error('Chart element #areaChart not found')
            }
        } catch (error) {
            console.error('Error rendering area chart:', error)
        }
    }

    renderTicketDetailsTable() {
        const tbody = document.getElementById("ticketDetailsBody")
        tbody.innerHTML = ""

        this.statsData.ticketTypeStats.forEach((ticketType) => {
            const row = document.createElement("tr")
            row.innerHTML = `
                <td class="ticket-name">${ticketType.name || "N/A"}</td>
                <td class="ticket-price">${this.formatCurrency(ticketType.price || 0)}</td>
                <td class="ticket-total">${ticketType.totalQuantity}</td>
                <td class="ticket-sold"><span class="badge bg-success">${ticketType.soldQuantity}</span></td>
                <td class="ticket-unsold"><span class="badge bg-warning">${ticketType.unsoldQuantity}</span></td>
                <td class="ticket-checkin"><span class="badge bg-info">${ticketType.checkInCount}</span></td>
                <td class="ticket-rate">
                    <div class="progress" style="height: 20px;">
                        <div class="progress-bar bg-success" role="progressbar" style="width: ${ticketType.checkInRate}%;" aria-valuenow="${ticketType.checkInRate}" aria-valuemin="0" aria-valuemax="100">
                            ${ticketType.checkInRate.toFixed(1)}%
                        </div>
                    </div>
                </td>
            `
            tbody.appendChild(row)
        })
    }

    formatCurrency(value) {
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND",
        }).format(value)
    }
}

// Function để khởi tạo dashboard
async function initializeDashboard() {
    console.log("[v0] Dashboard initialization started")

    // Cleanup charts cũ nếu có
    if (window.dashboardInstance && typeof window.dashboardInstance.destroyCharts === 'function') {
        window.dashboardInstance.destroyCharts()
    }

    // Lấy eventId từ URL hoặc từ global variable
    const eventId = getEventIdFromUrl() || window.eventId
    
    if (!eventId) {
        console.error("[v0] Event ID not found")
        return
    }

    console.log("[v0] Loading dashboard for event ID:", eventId)

    try {
        // Gọi API để lấy dữ liệu thống kê
        const response = await fetch(`/api/dashboard/event/${eventId}/stats`)
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        const statsData = await response.json()
        console.log("[v0] Dashboard stats loaded:", statsData)
        console.log("[v0] Raw API Response:", JSON.stringify(statsData, null, 2))

        // Debug: Log từng KPI value
        console.log("[v0] KPI Values Debug:")
        console.log("  - totalRevenue:", statsData.totalRevenue)
        console.log("  - totalTicketsSold:", statsData.totalTicketsSold)
        console.log("  - totalAttendees:", statsData.totalAttendees)
        console.log("  - totalCheckIn:", statsData.totalCheckIn)
        console.log("  - checkInRate:", statsData.checkInRate)
        console.log("  - refundRate:", statsData.refundRate)
        console.log("  - unsoldRate:", statsData.unsoldRate)
        console.log("  - ticketTypeStats:", statsData.ticketTypeStats)
        console.log("  - revenueByType:", statsData.revenueByType)

        // Khởi tạo dashboard với dữ liệu từ API
        const dashboard = new DashboardStats(statsData)
        window.dashboardInstance = dashboard // Lưu instance để có thể cleanup sau
        
        // Export methods để debug
        window.forceRenderCheckInChart = () => {
            if (window.dashboardInstance) {
                window.dashboardInstance.forceRenderCheckInChart()
            }
        }
        
        window.forceResizeCharts = () => {
            if (window.dashboardInstance) {
                window.dashboardInstance.resizeCharts()
            }
        }
        
        window.debugCharts = () => {
            if (window.dashboardInstance) {
                console.log('📊 Dashboard Instance:', window.dashboardInstance)
                console.log('📈 Charts:', window.dashboardInstance.charts)
                console.log('📊 Stats Data:', window.dashboardInstance.statsData)
            }
        }
        
        // Test resize ngay sau khi load
        setTimeout(() => {
            if (window.dashboardInstance) {
                console.log('🧪 Testing initial resize...')
                window.dashboardInstance.resizeCharts()
            }
        }, 2000) // Tăng delay để đảm bảo charts đã render xong
        
    } catch (error) {
        console.error("[v0] Error loading dashboard stats:", error)
        
        // Hiển thị thông báo lỗi
        document.getElementById("totalRevenue").textContent = "Lỗi"
        document.getElementById("totalTicketsSold").textContent = "Lỗi"
        document.getElementById("totalAttendees").textContent = "Lỗi"
        document.getElementById("refundRate").textContent = "Lỗi"
        document.getElementById("unsoldRate").textContent = "Lỗi"
        
        console.error("[v0] No data available for dashboard")
    }
}

// Khởi tạo dashboard khi trang load (cho trường hợp load trực tiếp)
// document.addEventListener("DOMContentLoaded", initializeDashboard)

// Export function để SPA router có thể gọi
window.initializeDashboard = initializeDashboard

// Helper function để lấy eventId từ URL
function getEventIdFromUrl() {
    const pathParts = window.location.pathname.split('/')
    const eventIndex = pathParts.indexOf('event')
    if (eventIndex !== -1 && pathParts[eventIndex + 1]) {
        return pathParts[eventIndex + 1]
    }
    return null
}

