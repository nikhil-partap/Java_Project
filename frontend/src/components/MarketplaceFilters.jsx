import '../views/BuyerMarketplace.css'

export default function MarketplaceFilters({
  searchTerm,
  setSearchTerm,
  selectedCategory,
  setSelectedCategory,
  maxPrice,
  setMaxPrice,
  categories = []
}) {
  return (
    <div className="filters-container">
      <input
        type="text"
        placeholder="Search listings by title or description..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        className="filter-search-input"
      />

      <select
        value={selectedCategory}
        onChange={(e) => setSelectedCategory(e.target.value)}
        className="filter-category-select"
      >
        <option value="All">All Categories</option>
        {categories.map((cat) => (
          <option key={cat} value={cat}>
            {cat}
          </option>
        ))}
      </select>

      <div className="filter-price-group">
        <label className="filter-price-label">
          Max Price: <strong>₹{maxPrice}</strong>
        </label>
        <input
          type="range"
          min="100"
          max="50000"
          step="100"
          value={maxPrice}
          onChange={(e) => setMaxPrice(Number(e.target.value))}
        />
      </div>
    </div>
  )
}
