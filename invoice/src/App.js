import React, { useState, useEffect } from "react";
import { Plus, Trash2, Send, Loader } from "lucide-react";

export default function InvoiceGenerator() {
  const [formData, setFormData] = useState({
    customerName: "",
    customerPhone: "",
    customerEmail: "",
    customerAddress: "",
    invoiceStatus: "PAID",
    ownerMessage: "",
    items: [
      { name: "", price: "", quantity: "", discount: "", discountType: "FLAT" },
    ],
    applyItemDiscounts: false,
    applyOverallDiscount: false,
    overallDiscount: "",
    overallDiscountType: "FLAT",
    finalDiscount: "",
    finalDiscountType: "FLAT",
    paymentMethod: "CASH",
    paymentDetails: "",
    issuedBy: "",
    isCostPrice: false,
    enableLogging: true,
  });

  const [calculations, setCalculations] = useState({
    itemTotals: [],
    subtotalBeforeOverall: 0,
    overallDiscountAmount: 0,
    subtotalAfterOverall: 0,
    finalDiscountAmount: 0,
    finalAmount: 0,
  });

  const [validations, setValidations] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [password, setPassword] = useState("");
  const [isPasswordValid, setIsPasswordValid] = useState(false);

  const AUTHORIZED_SIGNATORIES = [
    "D.H.",
    "N.D.",
    "S.R.",
    "Customer",
  ];

  const PASSWORDS = {
  "D.H": process.env.REACT_APP_PASS_DH,
  "N.H": process.env.REACT_APP_PASS_ND,
  "S.R": process.env.REACT_APP_PASS_SR,
};

  useEffect(() => {
    calculateTotals();
  }, [formData]);

  const calculateTotals = () => {
    const itemTotals = [];
    let subtotalBeforeOverall = 0;

    formData.items.forEach((item) => {
      if (item.price && item.quantity) {
        const baseTotal = parseFloat(item.price) * parseInt(item.quantity);
        let itemDiscount = 0;

        if (formData.applyItemDiscounts && item.discount) {
          itemDiscount =
            item.discountType === "PERCENT"
              ? (baseTotal * parseFloat(item.discount)) / 100
              : parseFloat(item.discount);
        }

        const itemTotal = baseTotal - itemDiscount;
        itemTotals.push({
          baseTotal: baseTotal.toFixed(2),
          discount: itemDiscount.toFixed(2),
          total: itemTotal.toFixed(2),
        });
        subtotalBeforeOverall += itemTotal;
      } else {
        itemTotals.push({ baseTotal: "0.00", discount: "0.00", total: "0.00" });
      }
    });

    let overallDiscountAmount = 0;
    if (formData.applyOverallDiscount && formData.overallDiscount) {
      overallDiscountAmount =
        formData.overallDiscountType === "PERCENT"
          ? (subtotalBeforeOverall * parseFloat(formData.overallDiscount)) / 100
          : parseFloat(formData.overallDiscount);
    }

    const subtotalAfterOverall = subtotalBeforeOverall - overallDiscountAmount;

    let finalDiscountAmount = 0;
    if (formData.finalDiscount) {
      finalDiscountAmount =
        formData.finalDiscountType === "PERCENT"
          ? (subtotalAfterOverall * parseFloat(formData.finalDiscount)) / 100
          : parseFloat(formData.finalDiscount);
    }

    const finalAmount = Math.max(0, subtotalAfterOverall - finalDiscountAmount);

    setCalculations({
      itemTotals,
      subtotalBeforeOverall: subtotalBeforeOverall.toFixed(2),
      overallDiscountAmount: overallDiscountAmount.toFixed(2),
      subtotalAfterOverall: subtotalAfterOverall.toFixed(2),
      finalDiscountAmount: finalDiscountAmount.toFixed(2),
      finalAmount: finalAmount.toFixed(2),
    });
  };

  const addItem = () => {
    setFormData({
      ...formData,
      items: [
        ...formData.items,
        {
          name: "",
          price: "",
          quantity: "",
          discount: "",
          discountType: "FLAT",
        },
      ],
    });
  };

  const removeItem = (index) => {
    if (formData.items.length > 1) {
      setFormData({
        ...formData,
        items: formData.items.filter((_, i) => i !== index),
      });
    }
  };

  const updateItem = (index, field, value) => {
    const newItems = [...formData.items];
    newItems[index][field] = value;
    setFormData({ ...formData, items: newItems });
    validateField(`item_${index}_${field}`, value, field);
  };

  const updateField = (field, value) => {
    let updatedData = { ...formData, [field]: value };

    // If Cost Price is checked, automatically disable logging
    if (field === "isCostPrice" && value === true) {
      updatedData.enableLogging = false;
    }

    // If Cost Price is unchecked, re-enable logging
    if (field === "isCostPrice" && value === false) {
      updatedData.enableLogging = true;
    }

    // Reset password validation when signatory changes
    if (field === "issuedBy") {
      setPassword("");
      setIsPasswordValid(false);

      // If Customer is selected, set invoice status to ORDER_PLACED and reset all prices to 0
      if (value === "Customer") {
        updatedData.invoiceStatus = "ORDER_PLACED";
        updatedData.items = updatedData.items.map((item) => ({
          ...item,
          price: "0",
          discount: "0",
        }));
        updatedData.overallDiscount = "0";
        updatedData.finalDiscount = "0";
        updatedData.applyItemDiscounts = false;
        updatedData.applyOverallDiscount = false;
      }
    }

    setFormData(updatedData);
    validateField(field, value);
  };

  const handlePasswordChange = (value) => {
    setPassword(value);
    if (
      PASSWORDS[formData.issuedBy] &&
      value === PASSWORDS[formData.issuedBy]
    ) {
      setIsPasswordValid(true);
    } else {
      setIsPasswordValid(false);
    }
  };

  const validateField = (fieldName, value, fieldType = fieldName) => {
    let isValid = true;

    // Skip validation for boolean fields (checkboxes)
    if (typeof value === "boolean") {
      return;
    }

    if (fieldType === "customerEmail") {
      isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) || value === "";
    } else if (fieldType === "customerPhone") {
      isValid = /^\d{10}$/.test(value) || value === "";
    } else if (fieldType === "price" || fieldType === "quantity") {
      isValid = !value || parseFloat(value) > 0;
    } else if (
      fieldType === "discount" ||
      fieldType === "overallDiscount" ||
      fieldType === "finalDiscount"
    ) {
      isValid = !value || parseFloat(value) >= 0;
    } else {
      isValid = value && typeof value === "string" && value.trim() !== "";
    }

    setValidations((prev) => ({ ...prev, [fieldName]: isValid }));
  };

  const getInputClass = (fieldName) => {
    const baseClass =
      "w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 transition-all";
    if (validations[fieldName] === undefined)
      return `${baseClass} border-gray-300 focus:ring-teal-500`;
    return validations[fieldName]
      ? `${baseClass} border-green-500 bg-green-50 focus:ring-green-500`
      : `${baseClass} border-red-500 bg-red-50 focus:ring-red-500`;
  };

  const validateForm = () => {
    const requiredFields = [
      "customerName",
      "customerPhone",
      "customerEmail",
      "customerAddress",
      "issuedBy",
    ];
    let isValid = true;
    const newValidations = {};

    requiredFields.forEach((field) => {
      const value = formData[field];
      let fieldValid = value && value.trim() !== "";

      if (field === "customerEmail" && fieldValid) {
        fieldValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
      }
      if (field === "customerPhone" && fieldValid) {
        fieldValid = /^\d{10}$/.test(value);
      }

      newValidations[field] = fieldValid;
      if (!fieldValid) isValid = false;
    });

    // Check password validation for non-Customer signatories
    if (
      formData.issuedBy &&
      formData.issuedBy !== "Customer" &&
      !isPasswordValid
    ) {
      alert("Please enter the correct password for the selected signatory");
      isValid = false;
    }

    const isCustomer = formData.issuedBy === "Customer";

    formData.items.forEach((item, index) => {
      // For Customer, only validate name and quantity, not price
      const fieldsToValidate = isCustomer
        ? ["name", "quantity"]
        : ["name", "price", "quantity"];

      fieldsToValidate.forEach((field) => {
        const fieldKey = `item_${index}_${field}`;
        const value = item[field];
        let fieldValid = value && value.toString().trim() !== "";

        if ((field === "price" || field === "quantity") && fieldValid) {
          fieldValid = parseFloat(value) > 0;
        }

        newValidations[fieldKey] = fieldValid;
        if (!fieldValid) isValid = false;
      });
    });

    // Only validate payment details if authorized signatory with valid password
    if (
      formData.issuedBy !== "Customer" &&
      isPasswordValid &&
      formData.paymentMethod === "ONLINE"
    ) {
      const hasDetails =
        formData.paymentDetails && formData.paymentDetails.trim() !== "";
      newValidations["paymentDetails"] = hasDetails;
      if (!hasDetails) isValid = false;
    }

    setValidations(newValidations);
    return isValid;
  };

  const handleSubmit = () => {
    if (!validateForm()) {
      alert("Please fill all required fields correctly");
      return;
    }

    // Extra check for password validation
    if (
      formData.issuedBy &&
      formData.issuedBy !== "Customer" &&
      !isPasswordValid
    ) {
      alert("Invalid password for the selected signatory");
      return;
    }

    setShowConfirmation(true);
  };

  const confirmAndSend = async () => {
    setShowConfirmation(false);
    setIsSubmitting(true);

    const today = new Date();
    const invoiceDate = `${today.getDate().toString().padStart(2, "0")}-${(
      today.getMonth() + 1
    )
      .toString()
      .padStart(2, "0")}-${today.getFullYear()}`;

    const isCustomer = formData.issuedBy === "Customer";

    const payload = {
      customerName: formData.customerName,
      customerPhone: formData.customerPhone,
      customerEmail: formData.customerEmail,
      customerAddress: formData.customerAddress,
      invoiceStatus: formData.invoiceStatus,
      ownerMessage: formData.ownerMessage || "",
      invoiceDate: invoiceDate,
      items: formData.items.map((item) => ({
        name: item.name,
        price: isCustomer ? 0 : parseFloat(item.price),
        quantity: parseInt(item.quantity),
        discount: isCustomer
          ? 0
          : formData.applyItemDiscounts
          ? parseFloat(item.discount) || 0
          : 0,
        discountType: formData.applyItemDiscounts ? item.discountType : "FLAT",
      })),
      applyOverallDiscount: isCustomer ? false : formData.applyOverallDiscount,
      overallDiscount: isCustomer
        ? 0
        : formData.applyOverallDiscount
        ? parseFloat(formData.overallDiscount) || 0
        : 0,
      overallDiscountType: formData.applyOverallDiscount
        ? formData.overallDiscountType
        : "FLAT",
      adjustmentAmount: isCustomer
        ? 0
        : parseFloat(formData.finalDiscount) || 0,
      adjustmentAmountType: formData.finalDiscountType,
      paymentMethod: formData.paymentMethod,
      paymentDetails:
        formData.paymentMethod === "ONLINE" ? formData.paymentDetails : "",
      issuedBy: formData.issuedBy,
      enableLogging: formData.enableLogging,
    };

    try {
      const response = await fetch("http://localhost:8080/invoice/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        alert(`Success: ${await response.text()}`);
        setFormData({
          customerName: "",
          customerPhone: "",
          customerEmail: "",
          customerAddress: "",
          invoiceStatus: "PAID",
          ownerMessage: "",
          items: [
            {
              name: "",
              price: "",
              quantity: "",
              discount: "",
              discountType: "FLAT",
            },
          ],
          applyItemDiscounts: false,
          applyOverallDiscount: false,
          overallDiscount: "",
          overallDiscountType: "FLAT",
          finalDiscount: "",
          finalDiscountType: "FLAT",
          paymentMethod: "CASH",
          paymentDetails: "",
          issuedBy: "",
          enableLogging: true,
        });
        setValidations({});
        setPassword("");
        setIsPasswordValid(false);
      } else {
        alert(`Backend Error: ${await response.text()}`);
      }
    } catch (error) {
      alert(`Network Error: ${error.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const isCustomer = formData.issuedBy === "Customer";
  const showPaymentAndLogging =
    formData.issuedBy && formData.issuedBy !== "Customer" && isPasswordValid;

  return (
    <div className="min-h-screen bg-gradient-to-br from-teal-50 via-emerald-50 to-green-50 p-4 sm:p-6">
      <div className="max-w-7xl mx-auto">
        <div className="bg-white rounded-xl shadow-lg mb-4 sm:mb-6 overflow-hidden border-t-4 border-teal-600">
          <div className="bg-gradient-to-r from-teal-600 to-emerald-600 text-white p-4 sm:p-6">
            <h1 className="text-2xl sm:text-4xl font-bold">
              The Tinkori Tales
            </h1>
            <p className="text-teal-100 text-sm sm:text-lg mt-1">
              Crafting Stories, Building Memories
            </p>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-lg p-4 sm:p-6">
          <div className="mb-6">
            <h2 className="text-lg sm:text-xl font-bold text-gray-800 mb-3 border-b-2 border-teal-200 pb-2">
              Customer Details
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div>
                <label className="block text-xs sm:text-sm font-semibold text-gray-700 mb-2">
                  Name *
                </label>
                <input
                  type="text"
                  value={formData.customerName}
                  onChange={(e) => updateField("customerName", e.target.value)}
                  className={getInputClass("customerName")}
                  placeholder="John Doe"
                />
              </div>
              <div>
                <label className="block text-xs sm:text-sm font-semibold text-gray-700 mb-2">
                  Phone *
                </label>
                <input
                  type="tel"
                  value={formData.customerPhone}
                  onChange={(e) => updateField("customerPhone", e.target.value)}
                  className={getInputClass("customerPhone")}
                  placeholder="9876543210"
                  maxLength="10"
                />
              </div>
              <div>
                <label className="block text-xs sm:text-sm font-semibold text-gray-700 mb-2">
                  Email *
                </label>
                <input
                  type="email"
                  value={formData.customerEmail}
                  onChange={(e) => updateField("customerEmail", e.target.value)}
                  className={getInputClass("customerEmail")}
                  placeholder="customer@example.com"
                />
              </div>
              <div>
                <label className="block text-xs sm:text-sm font-semibold text-gray-700 mb-2">
                  Address *
                </label>
                <input
                  type="text"
                  value={formData.customerAddress}
                  onChange={(e) =>
                    updateField("customerAddress", e.target.value)
                  }
                  className={getInputClass("customerAddress")}
                  placeholder="123 Main St"
                />
              </div>
            </div>
          </div>

          <div className="mb-6">
            <h2 className="text-lg sm:text-xl font-bold text-gray-800 mb-3 border-b-2 border-teal-200 pb-2">
              Invoice Information
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs sm:text-sm font-semibold text-gray-700 mb-2">
                  Invoice Status *{" "}
                  {isCustomer && (
                    <span className="text-teal-600">(Order Placed)</span>
                  )}
                </label>
                <select
                  value={formData.invoiceStatus}
                  onChange={(e) => updateField("invoiceStatus", e.target.value)}
                  disabled={isCustomer}
                  className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-teal-500 ${
                    isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                  }`}
                >
                  <option value="PAID">PAID</option>
                  <option value="QUOTATION">QUOTATION</option>
                  <option value="ORDER_PLACED">ORDER PLACED</option>
                </select>
              </div>
              <div>
                <label className="block text-xs sm:text-sm font-semibold text-gray-700 mb-2">
                  Owner Message (Optional)
                </label>
                <input
                  type="text"
                  value={formData.ownerMessage}
                  onChange={(e) => updateField("ownerMessage", e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-teal-500"
                  placeholder="Thank you!"
                />
              </div>
            </div>
          </div>

          <div className="mb-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-3 border-b-2 border-teal-200 pb-2 gap-2">
              <h2 className="text-lg sm:text-xl font-bold text-gray-800">
                Items
              </h2>
              <div className="flex flex-col sm:flex-row gap-2">
                <label className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-gray-700">
                  <input
                    type="checkbox"
                    checked={formData.applyItemDiscounts}
                    onChange={(e) =>
                      updateField("applyItemDiscounts", e.target.checked)
                    }
                    disabled={isCustomer}
                    className="w-4 h-4"
                  />
                  Apply Item Discounts
                </label>
                <button
                  onClick={addItem}
                  className="flex items-center gap-2 px-3 py-2 bg-teal-600 text-white text-sm rounded-lg hover:bg-teal-700"
                >
                  <Plus className="w-4 h-4" />
                  Add Item
                </button>
              </div>
            </div>

            <div className="space-y-3">
              {formData.items.map((item, index) => (
                <div key={index} className="bg-gray-50 p-3 rounded-lg border">
                  <div className="grid gap-3">
                    <input
                      type="text"
                      value={item.name}
                      onChange={(e) =>
                        updateItem(index, "name", e.target.value)
                      }
                      className={getInputClass(`item_${index}_name`)}
                      placeholder="Item Name *"
                    />
                    <div className="grid grid-cols-2 sm:grid-cols-6 gap-2">
                      <input
                        type="number"
                        step="0.01"
                        value={item.price}
                        onChange={(e) =>
                          updateItem(index, "price", e.target.value)
                        }
                        disabled={isCustomer}
                        className={`${getInputClass(`item_${index}_price`)} ${
                          isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                        }`}
                        placeholder="Price *"
                      />
                      <input
                        type="number"
                        value={item.quantity}
                        onChange={(e) =>
                          updateItem(index, "quantity", e.target.value)
                        }
                        className={getInputClass(`item_${index}_quantity`)}
                        placeholder="Qty *"
                      />
                      {formData.applyItemDiscounts && (
                        <>
                          <input
                            type="number"
                            step="0.01"
                            value={item.discount}
                            onChange={(e) =>
                              updateItem(index, "discount", e.target.value)
                            }
                            disabled={isCustomer}
                            className={`w-full px-2 py-2 border rounded-md text-sm ${
                              isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                            }`}
                            placeholder="Disc"
                          />
                          <select
                            value={item.discountType}
                            onChange={(e) =>
                              updateItem(index, "discountType", e.target.value)
                            }
                            disabled={isCustomer}
                            className={`px-2 py-2 border rounded-md text-sm ${
                              isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                            }`}
                          >
                            <option value="FLAT">₹</option>
                            <option value="PERCENT">%</option>
                          </select>
                        </>
                      )}
                      <div className="px-2 py-2 bg-teal-100 border border-teal-300 rounded-md font-bold text-teal-700 text-sm">
                        ₹{calculations.itemTotals[index]?.total || "0.00"}
                      </div>
                      {formData.items.length > 1 && (
                        <button
                          onClick={() => removeItem(index)}
                          className="p-2 bg-red-100 text-red-600 rounded-lg"
                        >
                          <Trash2 className="w-4 h-4 mx-auto" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="mb-6 bg-gradient-to-r from-teal-50 to-emerald-50 p-4 rounded-xl border-2 border-teal-200">
            <div className="space-y-4">
              <div className="flex justify-between pb-3 border-b">
                <div className="text-sm font-semibold">Subtotal</div>
                <div className="text-xl font-bold">
                  ₹{calculations.subtotalBeforeOverall}
                </div>
              </div>
              <div>
                <label className="flex items-center gap-2 text-sm font-semibold mb-2">
                  <input
                    type="checkbox"
                    checked={formData.applyOverallDiscount}
                    onChange={(e) =>
                      updateField("applyOverallDiscount", e.target.checked)
                    }
                    disabled={isCustomer}
                    className="w-4 h-4"
                  />
                  Overall Discount
                </label>
                {formData.applyOverallDiscount && (
                  <div className="flex flex-wrap gap-2">
                    <input
                      type="number"
                      step="0.01"
                      value={formData.overallDiscount}
                      onChange={(e) =>
                        updateField("overallDiscount", e.target.value)
                      }
                      disabled={isCustomer}
                      className={`w-24 px-2 py-2 border rounded-md text-sm ${
                        isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                      }`}
                      placeholder="0"
                    />
                    <select
                      value={formData.overallDiscountType}
                      onChange={(e) =>
                        updateField("overallDiscountType", e.target.value)
                      }
                      disabled={isCustomer}
                      className={`w-20 px-2 py-2 border rounded-md text-sm ${
                        isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                      }`}
                    >
                      <option value="FLAT">₹</option>
                      <option value="PERCENT">%</option>
                    </select>
                    <div className="text-orange-600 font-bold">
                      -₹{calculations.overallDiscountAmount}
                    </div>
                  </div>
                )}
              </div>
              <div className="pt-3 border-t">
                <label className="text-sm font-semibold mb-2 block">
                  Adjustment
                </label>
                <div className="flex flex-wrap gap-2">
                  <input
                    type="number"
                    step="0.01"
                    value={formData.finalDiscount}
                    onChange={(e) =>
                      updateField("finalDiscount", e.target.value)
                    }
                    disabled={isCustomer}
                    className={`w-24 px-2 py-2 border rounded-md text-sm ${
                      isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                    }`}
                    placeholder="0"
                  />
                  <select
                    value={formData.finalDiscountType}
                    onChange={(e) =>
                      updateField("finalDiscountType", e.target.value)
                    }
                    disabled={isCustomer}
                    className={`w-20 px-2 py-2 border rounded-md text-sm ${
                      isCustomer ? "bg-gray-100 cursor-not-allowed" : ""
                    }`}
                  >
                    <option value="FLAT">₹</option>
                    <option value="PERCENT">%</option>
                  </select>
                  {formData.finalDiscount && (
                    <div className="text-red-600 font-bold">
                      -₹{calculations.finalDiscountAmount}
                    </div>
                  )}
                </div>
              </div>
              <div className="bg-gradient-to-r from-green-500 to-emerald-500 text-white px-6 py-3 rounded-xl shadow-lg mt-4">
                <div className="flex justify-between">
                  <div className="text-sm font-semibold">Final Amount</div>
                  <div className="text-3xl font-bold">
                    ₹{calculations.finalAmount}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <div>
              <label className="block text-sm font-semibold mb-2">
                Authorized Signature *
              </label>
              <select
                value={formData.issuedBy}
                onChange={(e) => updateField("issuedBy", e.target.value)}
                className={getInputClass("issuedBy")}
              >
                <option value="">Select</option>
                {AUTHORIZED_SIGNATORIES.map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </select>
            </div>

            {formData.issuedBy && formData.issuedBy !== "Customer" && (
              <div>
                <label className="block text-sm font-semibold mb-2">
                  Password *
                </label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => handlePasswordChange(e.target.value)}
                  className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 ${
                    isPasswordValid
                      ? "border-green-500 bg-green-50 focus:ring-green-500"
                      : "border-gray-300 focus:ring-teal-500"
                  }`}
                  placeholder="Enter password"
                />
                {password && (
                  <p
                    className={`text-xs mt-1 ${
                      isPasswordValid ? "text-green-600" : "text-red-600"
                    }`}
                  >
                    {isPasswordValid
                      ? "✓ Password correct"
                      : "✗ Incorrect password"}
                  </p>
                )}
              </div>
            )}

            {showPaymentAndLogging && (
              <div className="col-span-full mb-4 p-4 bg-amber-50 rounded-lg border-2 border-amber-300">
                <label className="flex items-center gap-2 text-sm font-semibold text-amber-900">
                  <input
                    type="checkbox"
                    checked={formData.isCostPrice}
                    onChange={(e) =>
                      updateField("isCostPrice", e.target.checked)
                    }
                    className="w-4 h-4"
                  />
                  Cost Price Transaction? (Disables  logging)
                </label>
                {formData.isCostPrice && (
                  <p className="text-xs text-amber-700 mt-2">
                     Logging has been automatically disabled for this cost
                    price transaction
                  </p>
                )}
              </div>
            )}

            {showPaymentAndLogging && (
              <div>
                <label className="block text-sm font-semibold mb-2">
                  Payment Method
                </label>
                <select
                  value={formData.paymentMethod}
                  onChange={(e) => updateField("paymentMethod", e.target.value)}
                  className="w-full px-3 py-2 border rounded-md"
                >
                  <option value="CASH">Cash</option>
                  <option value="ONLINE">Online</option>
                </select>
              </div>
            )}

            {showPaymentAndLogging && formData.paymentMethod === "ONLINE" && (
              <div>
                <label className="block text-sm font-semibold mb-2">
                  Payment Details *
                </label>
                <input
                  type="text"
                  value={formData.paymentDetails}
                  onChange={(e) =>
                    updateField("paymentDetails", e.target.value)
                  }
                  className={getInputClass("paymentDetails")}
                  placeholder="UPI/Bank"
                />
              </div>
            )}
          </div>

          {showPaymentAndLogging && (
            <div className="mb-6 p-4 bg-yellow-50 rounded-lg border border-yellow-200">
              <label className="flex items-center gap-2 text-sm font-semibold">
                <input
                  type="checkbox"
                  checked={formData.enableLogging}
                  onChange={(e) =>
                    updateField("enableLogging", e.target.checked)
                  }
                  disabled={formData.isCostPrice}
                  className="w-4 h-4"
                />
                Enable Google Sheets Logging{" "}
                {formData.isCostPrice && "(Disabled for Cost Price)"}
              </label>
            </div>
          )}

          <div className="flex justify-center">
            <button
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-teal-600 to-emerald-600 text-white font-bold text-lg rounded-xl hover:from-teal-700 hover:to-emerald-700 disabled:opacity-50 shadow-xl"
            >
              {isSubmitting ? (
                <>
                  <Loader className="w-6 h-6 animate-spin" />
                  Processing...
                </>
              ) : (
                <>
                  <Send className="w-6 h-6" />
                  {isCustomer ? "Place Order" : "Generate Invoice"}
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {showConfirmation && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4">
            <h3 className="text-2xl font-bold mb-4">
              {isCustomer ? "Confirm Order" : "Confirm Invoice"}
            </h3>
            <div className="space-y-2 mb-6 text-sm">
              <p>
                <strong>Customer:</strong> {formData.customerName}
              </p>
              <p>
                <strong>Status:</strong>{" "}
                {isCustomer ? "Order Placed" : formData.invoiceStatus}
              </p>
              <p>
                <strong>Amount:</strong>{" "}
                <span className="text-2xl font-bold text-green-600">
                  ₹{calculations.finalAmount}
                </span>
              </p>
              {showPaymentAndLogging && formData.isCostPrice && (
                <p className="text-amber-700">
                  <strong>Cost Price Transaction</strong>
                </p>
              )}
              {showPaymentAndLogging && (
                <p>
                  <strong>Logging:</strong>{" "}
                  {formData.enableLogging ? "Enabled" : "Disabled"}
                </p>
              )}
            </div>
            <div className="flex gap-4">
              <button
                onClick={() => setShowConfirmation(false)}
                className="flex-1 px-4 py-3 bg-gray-200 rounded-lg hover:bg-gray-300 font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={confirmAndSend}
                className="flex-1 px-4 py-3 bg-gradient-to-r from-teal-600 to-emerald-600 text-white rounded-lg hover:from-teal-700 hover:to-emerald-700 font-semibold"
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
