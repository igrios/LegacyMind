import { useState } from "react";

                    {edge.source}

                    {" → "}

                    {edge.target}

                  </div>
                )
              )
            }

          </div>
        )
      }

    </div>
  );
}

// =====================================================
// BUTTON STYLE
// =====================================================

function buttonStyle(background) {

  return {

    padding: "14px",

    borderRadius: "12px",

    border: "none",

    background,

    color: "white",

    cursor: "pointer",

    fontWeight: "bold",

    fontSize: "14px"
  };
}

export default App;